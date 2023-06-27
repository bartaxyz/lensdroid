package com.bartaxyz.lensdroid

import android.app.Application
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType

class LensdroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initializeWalletConnect()
    }

    private fun initializeWalletConnect() {
        val connectionType = ConnectionType.AUTOMATIC
        val projectId = "819195da9abd10361cb047648629fdbc"
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=${projectId}"
        val appMetaData = Core.Model.AppMetaData(
            name = "Lensdroid",
            description = "Your Android window into Lens",
            url = "kotlin.web3Modal.walletconnect.com",
            icons = listOf("https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png"),
            redirect = "kotlin-web3Modal://request"
        )

        CoreClient.initialize(
            relayServerUrl = serverUrl,
            connectionType = connectionType,
            application = this,
            metaData = appMetaData,
            onError = { error ->
                // Error will be thrown if there's an issue during initialization
            }
        )

        /*Web3Modal.initialize(
            init = Modal.Params.Init(CoreClient),
            onSuccess = {
                // Callback will be called if initialization is successful
            },
            onError = { error ->
                // Error will be thrown if there's an issue during initialization
            }
        )*/
    }
}