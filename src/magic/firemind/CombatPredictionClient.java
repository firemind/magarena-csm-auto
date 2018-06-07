package magic.firemind;

import io.grpc.ManagedChannel;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Stream;
//import io.grpc.internal.IoUtils;
//import io.grpc.netty.NettyChannelBuilder;
import magic.model.MagicAbility;
import magic.model.MagicGame;
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
import static java.lang.Math.toIntExact;

//import java.io.OutputStream;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;

public class CombatPredictionClient {

    private final PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;
    private final Model.ModelSpec attackModelSpec;
    private final Model.ModelSpec blockModelSpec;
    private final static EnumSet<MagicAbility> keywords = EnumSet.of(
                MagicAbility.Deathtouch,
                MagicAbility.DoubleStrike,
                MagicAbility.FirstStrike,
                MagicAbility.Flying,
                MagicAbility.Indestructible,
                MagicAbility.Lifelink,
                MagicAbility.Trample,
                MagicAbility.Vigilance,
                MagicAbility.Shadow,
                MagicAbility.Wither,
                MagicAbility.Exalted,
                MagicAbility.Infect,
                MagicAbility.BattleCry,
                MagicAbility.Afflict
                );
    private final static int MAX_CREATURE_INPUTS = 20;
    private final static int CREATURE_LENGTH = 2+toIntExact(keywords.size());
    private final static int LENGTH_ALL_CREATURES = MAX_CREATURE_INPUTS*CREATURE_LENGTH;
    private final static int MAX_ATTACKER_INPUTS = 20;
    private final static int MAX_BLOCKER_INPUTS = 20;

    private TensorShapeProto.Dim lifesDim1 = TensorShapeProto.Dim.newBuilder().setSize(2).build();
    private TensorShapeProto.Dim attackDim = TensorShapeProto.Dim.newBuilder().setSize(MAX_ATTACKER_INPUTS).build();
    private TensorShapeProto.Dim attackersDim1 = TensorShapeProto.Dim.newBuilder().setSize(LENGTH_ALL_CREATURES).build();
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

    public List<Float> extractCreature(Object[] creatures){
        final List<Float> list = new ArrayList<>(LENGTH_ALL_CREATURES);
        for(Object creature : creatures){
            final MagicPermanent p = (MagicPermanent) creature;
            final MagicPowerToughness pt = p.getPowerToughness();
            list.add(1.0f*pt.getPositivePower());
            list.add(1.0f*pt.getPositiveToughness());
            keywords.forEach(kw ->{
               list.add(p.hasAbility(kw) ? 1.0f : 0.0f);
            });
            if(list.size() >= LENGTH_ALL_CREATURES){
                break;
            }
        }
        while(list.size() < LENGTH_ALL_CREATURES)
            list.add(0.0f);

//        System.out.println(Arrays.stream(list).map(e->e.toString()).collect(Collectors.joining(", ")));
        return list;
    }

    public List<Float> extractPT(MagicDeclareAttackersResult attackers){
        final List<Float> list = new ArrayList<>(LENGTH_ALL_CREATURES);
        for(MagicPermanent attacker : attackers){
            final MagicPowerToughness pt = attacker.getPowerToughness();
            list.add(1.0f*pt.getPositivePower());
            list.add(1.0f*pt.getPositiveToughness());
            keywords.forEach(kw ->{
               list.add(attacker.hasAbility(kw) ? 1.0f : 0.0f);
            });
            if(list.size() >= LENGTH_ALL_CREATURES){
                break;
            }
        }
        while(list.size() < LENGTH_ALL_CREATURES)
            list.add(0.0f);

//        System.out.println(Arrays.stream(list).map(e->e.toString()).collect(Collectors.joining(", ")));
        return list;
    }

    public static List<Float> encodeAttacks(MagicDeclareAttackersResult attackers, Object[] availableAttackers) {
        List<MagicPermanent> attackerList = new ArrayList<>();
        for(Object a: availableAttackers)
            attackerList.add((MagicPermanent) a);
        final Float[] list = new Float[MAX_ATTACKER_INPUTS];
        Arrays.fill(list, 0.0f);
        attackers.stream().limit(MAX_ATTACKER_INPUTS).forEach(e -> {
          int i = attackerList.indexOf(e);
          if(i < list.length)
            list[i] = 1.0f;
        });
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
//                        System.err.println("Ignoring blocker at "+ix);

                    }
                }
            }
        }
        return Arrays.asList(list);
    }

    public List<Float> predictAttackWin(List<AttackRep> combatReps) {
        final TensorShapeProto.Dim batchDim = TensorShapeProto.Dim.newBuilder().setSize(combatReps.size()).build();
        final TensorShapeProto lifesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(lifesDim1).build();
        final TensorShapeProto attackShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(attackDim).build();
        final TensorShapeProto creaturesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(attackersDim1).build();
        final TensorProto.Builder lifesBuilder = TensorProto.newBuilder();
        final TensorProto.Builder poisonBuilder = TensorProto.newBuilder();
        final TensorProto.Builder attackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder availableAttackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder blockersBuilder = TensorProto.newBuilder();
        for(AttackRep combatRep: combatReps) {
            lifesBuilder
                .addFloatVal(combatRep.lifePlayer)
                .addFloatVal(combatRep.lifeOpponent);
            poisonBuilder
                    .addFloatVal(combatRep.poisonPlayer)
                    .addFloatVal(combatRep.poisonOpponent);
            attackersBuilder
                    .addAllFloatVal(combatRep.attackers);
            availableAttackersBuilder
                    .addAllFloatVal(combatRep.availableCreatures);
            blockersBuilder
                    .addAllFloatVal(combatRep.blockers);
        }
        Predict.PredictRequest request = Predict.PredictRequest.newBuilder()
           .setModelSpec(attackModelSpec).
                putInputs("lifes", lifesBuilder.
                   setTensorShape(lifesShape).setDtype(dt).build()).
                putInputs("poison", poisonBuilder.
                        setTensorShape(lifesShape).setDtype(dt).build()).
                putInputs("attackers", attackersBuilder
                   .setTensorShape(attackShape).setDtype(dt).build()).
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
        final TensorProto.Builder poisonBuilder = TensorProto.newBuilder();
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
            poisonBuilder
                    .addFloatVal(combatRep.poisonPlayer)
                    .addFloatVal(combatRep.poisonOpponent);
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
                putInputs("poison", poisonBuilder.
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
        private final int poisonPlayer;
        private final int poisonOpponent;
        private final List<Float> attackers;
        private final List<Float> availableCreatures;
        private final List<Float> blockers;

        public AttackRep(int lifePlayer, int lifeOpponent, int poisonPlayer, int poisonOpponent, List<Float> attackers, List<Float> availableCreatures, List<Float> blockers){

            this.lifePlayer = lifePlayer;
            this.lifeOpponent = lifeOpponent;
            this.poisonPlayer = poisonPlayer;
            this.poisonOpponent = poisonOpponent;
            this.attackers = attackers;
            this.availableCreatures = availableCreatures;
            this.blockers = blockers;
        }
    }
    public class BlockRep {
        private final int lifePlayer;
        private final int lifeOpponent;
        private final int poisonPlayer;
        private final int poisonOpponent;
        private final List<Float> attackers;
        private final List<Float> availableBlockers;
        private final List<Float> oppCreatures;
        private final List<Float> blocks;

        public BlockRep(int lifePlayer, int lifeOpponent, int poisonPlayer, int poisonOpponent, List<Float> attackers, List<Float> availableBlockers, List<Float> blocks, List<Float> oppCreatures){

            this.lifePlayer = lifePlayer;
            this.lifeOpponent = lifeOpponent;
            this.poisonPlayer = poisonPlayer;
            this.poisonOpponent = poisonOpponent;
            this.attackers = attackers;
            this.availableBlockers = availableBlockers;
            this.oppCreatures = oppCreatures;
            this.blocks = blocks;
        }
    }
}
