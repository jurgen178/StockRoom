package com.thecloudsite.stockroom.invaders

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED
import android.media.SoundPool
import android.util.Log
import java.io.IOException

class SoundPlayer(context: Context) {

    // For sound FX
    var attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(FLAG_AUDIBILITY_ENFORCED)
                    .build()

    private val soundPool: SoundPool = SoundPool.Builder()
                                    .setAudioAttributes(attributes)
                                    .setMaxStreams(10)
                                    .build();

    companion object {
        // Loaded sounds
        var playerExplodeID = -1
        var invaderExplodeID = -1
        var shootID = -1
        var damageShelterID = -1
        var uhID = -1
        var ohID = -1
        var playerMoveID = -1

        // StreamIDs of sound loops
        val loopStreams = mutableMapOf<Int, Int>()
    }

    init {
        try {
            // Create objects of the 2 required classes
            val assetManager = context.assets
            var descriptor: AssetFileDescriptor


            // Load our fx in memory ready for use
            descriptor = assetManager.openFd("shoot.ogg")
            shootID = soundPool.load(descriptor, 0)

            descriptor = assetManager.openFd("invaderexplode.ogg")
            invaderExplodeID = soundPool.load(descriptor, 0)

            descriptor = assetManager.openFd("damageshelter.ogg")
            damageShelterID = soundPool.load(descriptor, 0)

            descriptor = assetManager.openFd("playerexplode.ogg")
            playerExplodeID = soundPool.load(descriptor, 0)

            descriptor = assetManager.openFd("damageshelter.ogg")
            damageShelterID = soundPool.load(descriptor, 0)

            descriptor = assetManager.openFd("uh.ogg")
            uhID = soundPool.load(descriptor, 0)

            descriptor = assetManager.openFd("oh.ogg")
            ohID = soundPool.load(descriptor, 0)

//            descriptor = assetManager.openFd("Tank-SoundBible.com-1359027625.wav")
//            playerMoveID = soundPool.load(descriptor, 0)


        } catch (e: IOException) {
            // Print an error message to the console
            Log.e("error", "failed to load sound files")
        }
    }

    fun playSound(id: Int){
        // TODO: stereo sound with left an right volume
        soundPool.play(id, 1f, 1f, 0, 0, 1f)
    }

    // This doesn't work. SoundPool bug causing it to not loop??
//    fun loopSound(id: Int, play: Boolean) {
//        if(loopStreams[id] == null) {
//            val streamId = soundPool.play(id, 1f, 1f, 0, 0, 1f)
//            soundPool.setLoop(streamId, -1)
//            loopStreams[id] = streamId
//        }
//
//        if(play) {
//            soundPool.resume(loopStreams[id]!!)
//        }
//        else {
//            soundPool.pause(loopStreams[id]!!)
//        }
//    }
}
