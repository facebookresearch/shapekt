/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package playground

import org.diffkt.*
import org.diffkt.model.Optimizer
import org.diffkt.model.SGDOptimizer
import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType
import kotlin.random.Random

/**
 * Demo of shape-checking a simple neural network (3 dense layers with relu activation functions) on MNIST.
 * It doesn't learn very well, and is just meant to demonstrate some practical use for shape-checking.
 *
 * Some comments below highlight possible shape-mismatch cases.
 * Also see [MNISTModel], [Dense], [Data] for uses of applying shape labels to classes.
 */

fun main() {
    // Load datasets
    val loader = MNISTDataLoader()
    val test = loader.getTestExamples()
    val train = loader.getTrainExamples()

    // Layers
    val l1 = Dense(784, 100, Random(0))
    val l2 = Dense(100, 20, Random(1))
    val l3 = Dense(20, 10, Random(2))

    val optim = SGDOptimizer<MNISTModel>(0.0001f, true, true)
    var model = MNISTModel(l1, l2, l3)

    // This fails since l2 doesn't accept the right input shape
    // val modelWrongOrder = MNISTModel(l2, l2, l3)

    // This fails because the composition is invalid
    // val modelBadFit = MNISTModel(l1, l3, l2)

    for (batch in 1..5000) {
        // Ideally we would cycle through batches but can't handle this as a compile time constant.
        // So instead, we keep drawing random samples for each batch
        train.shuffle()
        val trainFeatures = train.features
        val trainLabels = train.labels

        val batchStart = 0
        val batchEnd = batchStart + 10000

        val batchFeatures = slice(trainFeatures, batchStart, batchEnd, 0)
        val batchLabels = slice(trainLabels, batchStart, batchEnd, 0)

        model = train(
            optim,
            batchFeatures,
            batchLabels,
            model,
            printLoss = batch == 1 || batch % 10 == 0
        ).first
    }

    // This fails when the features and labels are mismatched:
    // train(optim, testFeatures, trainLabels, model)

}

// TODO (#103): These parameters should be checked even if there's no return type SType
@SType("BatchSize: Dim")
@AllowUnreduced
fun train(
    optim: Optimizer<MNISTModel>,
    features: @SType("[BatchSize, 28, 28]") FloatTensor,
    labels: @SType("[BatchSize, 10]") FloatTensor,
    model: MNISTModel,
    printLoss: Boolean = false
): Pair<MNISTModel, @SType("Dim") Int> {
    val (loss, tangent) = primalAndReverseDerivative(
        x = model,
        f = { m: MNISTModel -> loss(features, labels, m) },
        extractDerivative = { m: MNISTModel,
                              loss: DScalar,
                              extractor: (input: DTensor, output: DTensor) -> DTensor ->
            m.extractTangent(loss, extractor)
        })
    if (printLoss) println(loss)
    return Pair(optim.train(model, tangent), 0)
}

fun loss(features: DTensor, labels: DTensor, model: MNISTModel): DScalar {
    val prediction = model.predict(features)
    return crossEntropyLossFromOneHot(prediction, labels)
}