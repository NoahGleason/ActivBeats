package com.sdhacks.activbeats

import com.sdhacks.activbeats.wav.WavFile

class Sample(
    val peak: Int,
    private val duration: Double,
    val start: Double,
    private val data: DoubleArray,
    sampleRate: Long,
    val instrument: MainActivity.Instrument
) {

    class SampleFactory(wav: WavFile, var instrument: MainActivity.Instrument) {

        private var data : DoubleArray? = null
        private val naturalDuration : Double
        val validBits : Int = wav.validBits
        val sampleRate = wav.sampleRate

        init {
            val arrayListData = WavFile.getRaw(wav, 0)
            naturalDuration = arrayListData.size.toDouble() / wav.sampleRate
            data = DoubleArray(arrayListData.size)
            for (i in arrayListData.indices){
                data!![i] = arrayListData[i]
            }
        }

        fun getSample(peak : Int, duration: Long, start: Long) : Sample {
//            return Sample(peak, duration.toDouble() / 1000.0, start.toDouble() / 1000.0, data!!, sampleRate)
            return Sample(peak, naturalDuration, start.toDouble() / 1000.0, data!!, sampleRate, instrument)

        }
    }

    private val ratio = (data.size - 1) / duration
    private val startingSample = start * sampleRate
    private val naturalDuration: Double = data.size.toDouble() / sampleRate.toDouble()
    private val speedFrac : Double

    init {
        speedFrac = if (duration > naturalDuration){
            1.0/kotlin.math.round(duration / naturalDuration)
        } else {
            kotlin.math.round(naturalDuration / duration)
        }
    }

    private val endSample = startingSample + (data.size - 1)/speedFrac

    fun getValueAtTime(t : Double) : Double{
        if (t < start || t > start + duration){
            return 0.0
        }
        val index = (t - start) * ratio
        return peak.toDouble() * (data[index.toInt()] + (data[kotlin.math.ceil(index).toInt()] - data[index.toInt()]) * (index - index.toInt()))
    }

    fun getClipAtFrame(t : Long) : Double {
        if (t < startingSample || t > endSample){
            return 0.0
        }
        return peak.toDouble() * data[((t-startingSample) * speedFrac).toInt()]
    }

    override fun toString(): String {
        return "Sample(peak=$peak, duration=$duration, start=$start)"
    }


}