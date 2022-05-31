/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package playground

import org.diffkt.*
import org.diffkt.model.*
import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType
import kotlin.random.Random

@SType("A: Dim, B: Dim")
@AllowUnreduced
class MNISTModel(
    val l1: @SType("784, A") Dense,
    val l2: @SType("A, B") Dense,
    val l3: @SType("B, 10") Dense,
) : Model<MNISTModel>() {
    override val layers: List<Layer<*>> = listOf(l1, l2, l3)
    override fun withLayers(newLayers: List<Layer<*>>): MNISTModel =
        MNISTModel(
            newLayers[0] as @SType("784, A") Dense,
            newLayers[1] as @SType("A, B") Dense,
            newLayers[2] as @SType("B, 10") Dense
        )

    @SType("N: Dim")
    override fun predict(data: @SType("[N, 28, 28]") DTensor): @SType("[N, 10]") DTensor {
        val flattened = flatten(data)
        val postL1 = l1.invoke(flattened)
        val postL2 = l2.invoke(postL1)
        val postL3 = l3.invoke(postL2)
        return postL3.softmax(1)
    }

    override fun equals(other: Any?): Boolean = TODO()
    override fun hashCode(): Int = TODO()
}

@SType("A: Dim, B: Dim")
@AllowUnreduced
class Dense(
    val w: @SType("[A,B]") DTensor,
    val b: @SType("[B]") DTensor,
): TrainableLayerSingleInput<Dense> {

    constructor(
        inputs: @SType("A") Int,
        outputs: @SType("B") Int,
        random: Random,
    ): this(FloatTensor.random(random, Shape(inputs, outputs)), FloatTensor.random(random, Shape(outputs)))

    override val trainables: List<Trainable<*>>
        get() = listOf(TrainableTensor(w), TrainableTensor(b))

    @SType("S: Shape")
    override fun invoke(input: @SType("S") DTensor): @SType("matmul(S,[A,B])") DTensor {
        return (input.matmul(w) + b).relu()
    }

    override fun wrap(wrapper: Wrapper): @SType("A, B") Dense =
        Dense(wrapper.wrap(w), wrapper.wrap(b))

    override fun withTrainables(trainables: List<Trainable<*>>): Dense =
        Dense(
            (trainables[0] as TrainableTensor).tensor,
            (trainables[1] as TrainableTensor).tensor,
        )
}

@SType("S: Shape")
@AllowUnreduced
fun flatten(t: @SType("S") DTensor): @SType("flatten(S,1)") DTensor {
    return t.flatten(1, t.rank-1)
}

@SType("S: Shape, A: Dim, B: Dim, C: Dim")
@AllowUnreduced
fun slice(
    data: @SType("S") FloatTensor,
    start: @SType("A") Int,
    end: @SType("B") Int,
    axis: @SType("C") Int,
): @SType("slice(S, A, B, C)") FloatTensor {
    return data.slice(start, end, axis)
}