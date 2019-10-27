package com.example.bluetooth

import android.util.Log
import com.example.bluetooth.wav.WavFile

class Sample(
    val peak: Int,
    private val duration: Double,
    private val start: Double,
    private val data: DoubleArray,
    private val sampleRate: Long
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
//            return Sample(peak, duration.toDouble() / 1000.0, start.toDouble() / 1000.0, data!!)
            return Sample(peak, naturalDuration, start.toDouble() / 1000.0, data!!, sampleRate)

        }
    }

    private val ratio = data.size / duration
    private val startingSample = start * sampleRate
    private val endSample = startingSample + data.size - 1

    fun getValueAtTime(t : Double) : Double{
        if (t < start || t > start + duration){
            return 0.0
        }
        val index = kotlin.math.min((t - start) * ratio, data.size - 1.0)
        return peak.toDouble() * (data[index.toInt()] + (data[kotlin.math.ceil(index).toInt()] - data[index.toInt()]) * (index - index.toInt()))
    }

    fun getClipAtFrame(t : Long) : Double {
        if (t < startingSample || t > endSample){
            return 0.0
        }
        return data[(t-startingSample).toInt()]
    }

    override fun toString(): String {
        return "Sample(peak=$peak, duration=$duration, start=$start)"
    }


}