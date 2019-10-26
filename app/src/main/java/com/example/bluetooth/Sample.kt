package com.example.bluetooth

import com.example.bluetooth.wav.WavFile

class Sample(private val peak: Int, private val duration: Double, private val start: Double, private val data : DoubleArray) {


    class SampleFactory(wav: WavFile) {

        private var data : DoubleArray? = null

        init {
            val arrayListData = WavFile.getRaw(wav, 0)
            data = DoubleArray(arrayListData.size)
            for (i in arrayListData.indices){
                data!![i] = arrayListData[i]
            }
        }

        fun getSample(peak : Int, duration: Long, start: Long) : Sample {
            return Sample(peak, duration.toDouble() / 1000., start.toDouble() / 1000., data!!)
        }
    }

    fun getValueAtTime(t : Double) : Double{
        if (t < start)
    }

}