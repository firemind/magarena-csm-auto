package magic.firemind;

import io.grpc.ManagedChannel;
import io.grpc.internal.IoUtils;
import io.grpc.netty.NettyChannelBuilder;
import magic.model.MagicPowerToughness;
import magic.model.choice.MagicCombatCreature;
import magic.model.choice.MagicDeclareAttackersResult;
import org.tensorflow.framework.DataType;
import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import magic.model.MagicPermanent;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CombatPredictionClient {

    private ManagedChannel channel;
    private PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;
    private final Model.ModelSpec modelSpec;
    private final static int MAX_CREATURE_INPUTS = 10;
    private final static int MAX_PT_INPUTS = MAX_CREATURE_INPUTS*2;

    TensorShapeProto.Dim lifesDim1 = TensorShapeProto.Dim.newBuilder().setSize(2).build();
    TensorShapeProto.Dim attackersDim1 = TensorShapeProto.Dim.newBuilder().setSize(MAX_PT_INPUTS).build();
    org.tensorflow.framework.DataType dt = DataType.DT_FLOAT;

    public CombatPredictionClient() {
        channel = ManagedChannelBuilder
                .forAddress("localhost", 8500)
                .usePlaintext(true).maxInboundMessageSize(100 * 1024 * 1024).build();

        blockingStub =
                PredictionServiceGrpc.newBlockingStub(channel);
        modelSpec = Model.ModelSpec.newBuilder()
                .setName("default").setSignatureName("serving_default").build();
    }

    public List<Float> extractPT(Object[] attackers){
        final Float [] list = new Float[MAX_PT_INPUTS];
        int ix=0;
        for(Object attacker : attackers){
            final MagicPermanent p = (MagicPermanent) attacker;
            final MagicPowerToughness pt = p.getPowerToughness();
            list[ix++] = 1.0f*pt.getPositivePower();
            list[ix++] = 1.0f*pt.getPositiveToughness();
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
        }
        while(ix < MAX_PT_INPUTS)
            list[ix++] = 0.0f;

//        System.out.println(Arrays.stream(list).map(e->e.toString()).collect(Collectors.joining(", ")));
        return Arrays.asList(list);
    }

    public List<Float> predictWin(List<CombatRep> combatReps) {
        final TensorShapeProto.Dim batchDim = TensorShapeProto.Dim.newBuilder().setSize(combatReps.size()).build();

        final TensorShapeProto lifesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(lifesDim1).build();
        final TensorShapeProto creaturesShape = TensorShapeProto.newBuilder().addDim(batchDim).addDim(attackersDim1).build();
        final TensorProto.Builder lifesBuilder = TensorProto.newBuilder();
        final TensorProto.Builder attackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder availableAttackersBuilder = TensorProto.newBuilder();
        final TensorProto.Builder blockersBuilder = TensorProto.newBuilder();
        for(CombatRep combatRep: combatReps) {
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
           .setModelSpec(modelSpec).
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
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .predict(request);
//        System.out.println(response);
        return response.getOutputsOrThrow("win_percentage").getFloatValList();
    }
    public class CombatRep {
        private final int lifePlayer;
        private final int lifeOpponent;
        private final MagicDeclareAttackersResult attackers;
        private final List<Float> availableCreatures;
        private final List<Float> blockers;

        public CombatRep(int lifePlayer, int lifeOpponent, MagicDeclareAttackersResult attackers, List<Float> availableCreatures, List<Float> blockers){

            this.lifePlayer = lifePlayer;
            this.lifeOpponent = lifeOpponent;
            this.attackers = attackers;
            this.availableCreatures = availableCreatures;
            this.blockers = blockers;
        }

    }
}
