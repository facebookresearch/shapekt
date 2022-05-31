/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package playground

import org.diffkt.FloatTensor
import org.diffkt.concat
import org.diffkt.gather
import org.diffkt.Shape
import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

const val NumClasses = 10

class MNISTDataLoader(val oneHotLabels: Boolean = true) {
    private val numClasses = NumClasses
    val dataDirectory = System.getProperty("user.home") + "/.shapeTypingDemos/datasets/"
    val url = "http://yann.lecun.com/exdb/mnist/"

    val data = load()
    fun getTrainExamples(): @SType("60000, [28, 28], [10]") Data { return data.first }
    fun getTestExamples(): @SType("10000, [28, 28], [10]") Data { return data.second }

    private fun load(printout: Boolean = false): Pair<
            @SType("60000, [28, 28], [10]") Data, // training
            @SType("10000, [28, 28], [10]") Data // testing
    >
    {
        val files = listOf(
            "train-labels-idx1-ubyte.gz",
            "train-images-idx3-ubyte.gz",
            "t10k-labels-idx1-ubyte.gz",
            "t10k-images-idx3-ubyte.gz"
        )
        files.forEach {
            if (!Files.exists(Paths.get(dataDirectory + it)))
                downloadFile(url + it, dataDirectory, it)
        }

        val paths = files.map { dataDirectory + it }
        val trainImages = loadImageSet(paths[1])
        val trainLabels = loadLabelSet(paths[0])
        val testImages = loadImageSet(paths[3])
        val testLabels = loadLabelSet(paths[2])

        val trainExamples = Data(trainImages, trainLabels)
        val testExamples = Data(testImages, testLabels)

        if (printout) {
            printDatasetSize(trainExamples, "train")
            printDatasetSize(testExamples, "test")
        }

        return Pair(
            trainExamples as @SType("60000, [28, 28], [10]") Data,
            testExamples as @SType("10000, [28, 28], [10]") Data
        )
    }

    private fun loadImageSet(path: String): FloatTensor {
        val file = Paths.get(path).toFile()
        val stream = GZIPInputStream(FileInputStream(file))
        val buffer = ByteArray(16384)
        stream.read(buffer, 0, 16)
        val numImages = ByteBuffer.wrap(buffer.slice(4 until 8).toByteArray()).int
        val numRows = ByteBuffer.wrap(buffer.slice(8 until 12).toByteArray()).int
        val numColumns = ByteBuffer.wrap(buffer.slice(12 until 16).toByteArray()).int
        val data = FloatArray(numImages * numRows * numColumns)
        var off = 0
        var bytesIn: Int
        while (run { bytesIn = stream.read(buffer, 0, buffer.size); bytesIn >= 0 }) {
            var i = 0
            while (i < bytesIn) {
                val a = buffer[i].toInt()
                data[off] = (a and 0xFF) / 255f
                i += 1
                off += 1
            }
        }
        return FloatTensor(Shape(numImages, numRows, numColumns), data)
    }

    private fun oneHot(cls: Int): FloatArray {
        val a = FloatArray(numClasses) { 0f }
        a[cls] = 1f
        return a
    }

    private fun loadLabelSet(path: String): FloatTensor {
        val file = Paths.get(path).toFile()
        val stream = GZIPInputStream(FileInputStream(file))
        val buffer = ByteArray(8192) { Byte.MIN_VALUE }
        stream.read(buffer, 0, 8)
        val numLabels = ByteBuffer.wrap(buffer.slice(4 until 8).toByteArray()).int
        val data = if (oneHotLabels) FloatArray(numLabels * numClasses) else FloatArray(numLabels)
        var pos = 0
        var bytesIn: Int
        val labelSize = if (oneHotLabels) numClasses else 1
        while (run { bytesIn = stream.read(buffer, 0, buffer.size); bytesIn >= 0 }) {
            var i = 0
            while (i < bytesIn) {
                val label = buffer[i].toInt()
                if (oneHotLabels)
                    oneHot(label).copyInto(data, pos, 0, numClasses)
                else
                    data[pos] = label.toFloat()
                i += 1
                pos += labelSize
            }
        }
        return if (oneHotLabels)
            FloatTensor(Shape(numLabels, numClasses), data)
        else
            FloatTensor(Shape(numLabels), data)
    }

    private fun printDatasetSize(examples: Data, purpose: String) {
        println("$purpose set: ${examples.features.shape.first}" +
                " examples, dimensions ${examples.features.shape.drop(1)}, from $numClasses classes")
    }
}

/**
 * Downloads a file at the given url to the given directory with the given file name.
 *
 * Uses http/https proxy settings from env vars.
 */
fun downloadFile(url: String, directoryName: String, fileName: String) {
    propagateProxy()
    File(directoryName).mkdirs()
    val f = File("$directoryName/$fileName")
    val input = GZIPInputStream(URL(url).openStream())
    val output = GZIPOutputStream(FileOutputStream(f))
    val dataBuffer = ByteArray(1024)
    var bytesRead = input.read(dataBuffer, 0, 1024)
    while (bytesRead != -1) {
        output.write(dataBuffer, 0, bytesRead)
        bytesRead = input.read(dataBuffer, 0, 1024)
    }
    input.close()
    output.close()
}

/**
 * Propagates the HTTP_PROXY and HTTPS_PROXY env vars to Java system
 * properties for use by Java network calls.
 */
fun propagateProxy() {
    val httpProxy = System.getenv("http_proxy")
    if (httpProxy != null) {
        val proxyUrl = URL(httpProxy)
        System.setProperty("http.proxyHost", proxyUrl.host)
        System.setProperty("http.proxyPort", proxyUrl.port.toString())
    }
    val httpsProxy = System.getenv("https_proxy")
    if (httpsProxy != null) {
        val proxyUrl = URL(httpsProxy)
        System.setProperty("https.proxyHost", proxyUrl.host)
        System.setProperty("https.proxyPort", proxyUrl.port.toString())
    }
}

@SType("Size: Dim, FeatureShape: Shape, LabelShape: Shape")
@AllowUnreduced
class Data(
    features: @SType("concat([Size], FeatureShape)") FloatTensor,
    labels: @SType("concat([Size], LabelShape)") FloatTensor,
) {

    var features = features
        private set
    var labels = labels
        private set

    init {
        require(features.shape.first == labels.shape.first) {
            "The number of examples must match number of labels." +
                    " Got ${features.shape.first} examples and ${labels.shape.first} labels"
        }
    }

    val size = features.shape[0]

    /**
     * Shuffles individual examples within the Data
     */
    fun shuffle() {
        val numBatches = features.shape.first
        assert(labels.shape.first == numBatches)
        val permutation = (0 until numBatches).shuffled()
        val shuffledFeatures = features.gather(permutation, 0)
        val shuffledLabels = labels.gather(permutation, 0)
        features = shuffledFeatures
        labels = shuffledLabels
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Data) return false
        return this.features == other.features && this.labels == other.labels
    }
}