package com.onionchat.dr0id.connection

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onionchat.common.Crypto
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.queue.OnionTask
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.security.cert.Certificate
import javax.crypto.Cipher


@RunWith(AndroidJUnit4::class)
class ConnectionTest {


    @Test
    fun checkConnection() {

        val future = ConnectionManager.StateMachine.checkConnection()
        assertEquals(future.get().status, OnionTask.Status.FAILURE)
        SystemClock.sleep(20000)
        val future2 = ConnectionManager.StateMachine.checkConnection()
        assertEquals(future2.get().status, OnionTask.Status.SUCCESS)
    }
}