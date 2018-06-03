package magic.firemind;

import io.grpc.ManagedChannel;
//import io.grpc.internal.IoUtils;
//import io.grpc.netty.NettyChannelBuilder;
import magic.model.MagicPowerToughness;
import magic.model.choice.MagicCombatCreature;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.model.choice.MagicDeclareBlockersResult;
//import org.omg.SendingContext.RunTime;
import org.tensorflow.framework.DataType;
import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;
//import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import magic.model.MagicPermanent;

//import java.io.OutputStream;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;

public class CombatPredictionClient {

    private PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;
    private final Model.ModelSpec attackModelSpec;
    private final Model.ModelSpec blockModelSpec;
    private final static int MAX_CREATURE_INPUTS = 10;
    private final static int MAX_PT_INPUTS = MAX_CREATURE_INPUTS*2;
    private final static int MAX_ATTACKER_INPUTS = 10;
    private final static int MAX_BLOCKER_INPUTS = 10;

    private TensorShapeProto.Dim lifesDim1 = TensorShapeProto.Dim.newBuilder().setSize(2).build();
    private TensorShapeProto.Dim attackersDim1 = TensorShapeProto.Dim.newBuilder().setSize(MAX_PT_INPUTS).build();
    private TensorShapeProto.Dim blocksDim1 = TensorShapeProto.Dim.newBuilder().setSize(MAX_BLOCKER_INPUTS*(MAX_ATTACKER_INPUTS+1)).build();

    private org.tensorflow.framework.DataType dt = DataType.DT_FLOAT;

    public CombatPredictionClient() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("tf-combat-server", 8500)
                .usePlaintext(true).maxInboundMessageSize(100 * 1024 * 1024).build();

        blockingStub =
                PredictionServiceGrpc.newBlockingStub(channel);
        attackModelSpec = Model.ModelSpec.newBuilder()
                .setName("attacks").setSignatureName("serving_default").build();
        blockModelSpec = Model.ModelSpec.newBuilder()
                .setName("blocks").setSignatureName("serving_default").build();
    }

    public List<Float> extractPT(Object[] attackers){
        final Float [] list = new Float[MAX_PT_INPUTS];
        int ix=0;
        for(Object attacker : attackers){
            final MagicPermanent p = (MagicPermanent) attacker;
            final MagicPowerToughness pt = p.getPowerToughness();
            list[ix++] = 1.0f*pt.getPositivePower();
            list[ix++] = 1.0f*pt.getPositiveToughness();
            if(ix >= MAX_PT_INPUTS){
                break;
            }
        }
        while(ix < MAX_PT_INPUTS)
            list[ix++] = 0.0f;

//        System.out.println(Arrays.stream(list).map(e->e.toString()).collect(Collectors.joining(", ")));
        return Arrays.asList(list);
    }

    public List<Float> extractPT(MagicDeclareAttackersResult attackers){
        final Float [] list = new Float[MAX_PT_INPUTS];
        int ix=0;
        for(MagicPermanent attacker : attackers){
            final MagicPowerToughness pt = attacker.getPowerToughness();
            list[ix++] = 1.0f*pt.getPositivePower();
            list[ix++] = 1.0f*pt.getPositiveToughness();
            if(ix >= MAX_PT_INPUTS){
                break;
            }
        }
        while(ix < MAX_PT_INPUTS)
            list[ix++] = 0.0f;

//        System.out.println(Arrays.stream(list).map(e->e.toString()).collect(Collectors.joining(", ")));
        return Arrays.asList(list);
    }

    public List<Float> extractBlock(MagicDeclareBlockersResult block, Object[] attackers, Object[] availableBlockers){
        final Float [] list = new Float[(MAX_ATTACKER_INPUTS+1)*(MAX_BLOCKER_INPUTS)];
        for(int i=0; i<list.length;i++){
            if(i % MAX_BLOCKER_INPUTS == 0 && i < availableBlockers.length) {
                list[i] = 1.0f;
            }else{
                list[i] = 0.0f;
            }
        }

        for(MagicCombatCreature[] b: block) {
            MagicCombatCreature attacker = b[0];
            int aix = -1;
            int tmp = 0;
            for (Object pa : attackers) {
                if (attacker.permanent == pa) {
                    aix = tmp;
                }
                tmp++;
            }
            assert aix >= 0;
            if (b.length > 1) {
                for (int i = 1; i < Math.min(b.length,MAX_BLOCKER_INPUTS); i++) {
                    MagicCombatCreature blocker = b[i];
                    int bix = -1;
                    tmp = 0;
                    for (Object pb : availableBlockers) {
                        if (blocker.permanent == pb) {
                            bix = tmp;
                        }
                        tmp++;
                    }
                    if(bix < 0){
                        throw new RuntimeException(block.toString()+"\n"+"Block not found: "+blocker.permanent.toString()+ " in " + Arrays.toString(availableBlockers));
                    }
                    int ix = bix*MAX_BLOCKER_INPUTS + (aix+1);
                    if(ix < list.length) {
                        list[bix * MAX_BLOCKER_INPUTS] = 0.0f;
                        list[ix] = 1.0f;
                    }else{
                        System.err.println("Ignoring blocker at "+ix);

                    }
                }
            }
        }
        return Arrays.asList(list);
    }

    public List<Float> predictAttackWin(List<AttackRep> combatReps) {
        final TensorShapeProto.Dim batchDim = TensorShapeProto.Dim.newBuilder().setSize(combatReps.size()).build();
        final TensorShapeProto lifesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(lifesDim1).build();
        final TensorShapeProto creaturesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(attackersDim1).build();
        final TensorProto.Builder lifesBuilder = TensorProto.newBuilder();
        final TensorProto.Builder attackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder availableAttackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder blockersBuilder = TensorProto.newBuilder();
        for(AttackRep combatRep: combatReps) {
            lifesBuilder
                .addFloatVal(combatRep.lifePlayer)
                .addFloatVal(combatRep.lifeOpponent);
            attackersBuilder
                    .addAllFloatVal(extractPT(combatRep.attackers));
            availableAttackersBuilder
                    .addAllFloatVal(combatRep.availableCreatures);
            blockersBuilder
                    .addAllFloatVal(combatRep.blockers);
        }
        Predict.PredictRequest request = Predict.PredictRequest.newBuilder()
           .setModelSpec(attackModelSpec).
                putInputs("lifes", lifesBuilder.
                   setTensorShape(lifesShape).setDtype(dt).build()).
                putInputs("attackers", attackersBuilder
                   .setTensorShape(creaturesShape).setDtype(dt).build()).
                putInputs("available_attackers", availableAttackersBuilder
                   .setTensorShape(creaturesShape).setDtype(dt).build()).
                putInputs("blockers", blockersBuilder
                   .setTensorShape(creaturesShape).setDtype(dt).build()
                ).
                build();
        Predict.PredictResponse response = blockingStub
                .withDeadlineAfter(1, TimeUnit.SECONDS)
                .predict(request);
//        System.out.println(response);
        return response.getOutputsOrThrow("win_percentage").getFloatValList();
    }

    public List<Float> predictBlockWin(List<BlockRep> combatReps) {
        final TensorShapeProto.Dim batchDim = TensorShapeProto.Dim.newBuilder().setSize(combatReps.size()).build();
        final TensorShapeProto lifesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(lifesDim1).build();
        final TensorShapeProto creaturesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(attackersDim1).build();
        final TensorShapeProto blocksShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(blocksDim1).build();
        final TensorProto.Builder lifesBuilder = TensorProto.newBuilder();
        final TensorProto.Builder attackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder availableBlockersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder oppCreaturesBuilder = TensorProto.newBuilder();
        final TensorProto.Builder blocksBuilder = TensorProto.newBuilder();
        for(BlockRep combatRep: combatReps) {
//            for(int i=0; i<MAX_BLOCKER_INPUTS; i++){
//                System.err.print("Block: [");
//                for(int j=0; j<=MAX_ATTACKER_INPUTS; j++){
//                    System.err.print(combatRep.blocks.get(i*MAX_BLOCKER_INPUTS+j)+", ");
//                }
//                System.err.println("]");
//            }
            lifesBuilder
                .addFloatVal(combatRep.lifePlayer)
                .addFloatVal(combatRep.lifeOpponent);
            attackersBuilder
                    .addAllFloatVal(combatRep.attackers);
            availableBlockersBuilder
                    .addAllFloatVal(combatRep.availableBlockers);
            oppCreaturesBuilder
                    .addAllFloatVal(combatRep.oppCreatures);
            blocksBuilder
                    .addAllFloatVal(combatRep.blocks);
        }
        Predict.PredictRequest request = Predict.PredictRequest.newBuilder()
           .setModelSpec(blockModelSpec).
                putInputs("lifes", lifesBuilder.
                   setTensorShape(lifesShape).setDtype(dt).build()).
                putInputs("attackers", attackersBuilder
                   .setTensorShape(creaturesShape).setDtype(dt).build()).
                putInputs("available_blockers", availableBlockersBuilder
                   .setTensorShape(creaturesShape).setDtype(dt).build()).
                putInputs("opp_creatures", oppCreaturesBuilder
                        .setTensorShape(creaturesShape).setDtype(dt).build()).
                putInputs("blocks", blocksBuilder
                   .setTensorShape(blocksShape).setDtype(dt).build()
                ).
                build();
        Predict.PredictResponse response = blockingStub
                .withDeadlineAfter(1, TimeUnit.SECONDS)
                .predict(request);
//        System.out.println(response);
        return response.getOutputsOrThrow("win_percentage").getFloatValList();
    }
    public class AttackRep {
        private final int lifePlayer;
        private final int lifeOpponent;
        private final MagicDeclareAttackersResult attackers;
        private final List<Float> availableCreatures;
        private final List<Float> blockers;

        public AttackRep(int lifePlayer, int lifeOpponent, MagicDeclareAttackersResult attackers, List<Float> availableCreatures, List<Float> blockers){

            this.lifePlayer = lifePlayer;
            this.lifeOpponent = lifeOpponent;
            this.attackers = attackers;
            this.availableCreatures = availableCreatures;
            this.blockers = blockers;
        }
    }
    public class BlockRep {
        private final int lifePlayer;
        private final int lifeOpponent;
        private final List<Float> attackers;
        private final List<Float> availableBlockers;
        private final List<Float> oppCreatures;
        private final List<Float> blocks;

        public BlockRep(int lifePlayer, int lifeOpponent, List<Float> attackers, List<Float> availableBlockers, List<Float> blocks, List<Float> oppCreatures){

            this.lifePlayer = lifePlayer;
            this.lifeOpponent = lifeOpponent;
            this.attackers = attackers;
            this.availableBlockers = availableBlockers;
            this.oppCreatures = oppCreatures;
            this.blocks = blocks;
        }
    }
}
