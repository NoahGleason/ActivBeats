package com.example.bluetooth

import com.example.bluetooth.wav.WavFile

class Sample(
    val peak: Int,
    private val duration: Double,
    private val start: Double,
    private val data: DoubleArray
) {

    class SampleFactory(wav: WavFile) {

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
            return Sample(peak, duration.toDouble() / 1000.0, start.toDouble() / 1000.0, data!!)
        }
    }

    private val ratio = data.size / duration

    fun getValueAtTime(t : Double) : Double{
        if (t < start || t > start + duration){
            return 0.0
        }
        val index = (t - start) * ratio
        return peak.toDouble() * (data[index.toInt()] + (data[kotlin.math.ceil(index).toInt()] - data[index.toInt()]) * (index - index.toInt()))
    }

}