package me.orius.xtrust.config.types

import de.exlll.configlib.Configuration
import me.orius.xtrust.annotations.File

@Configuration
@File("netbird.yml")
class NetbirdConfig {
    var download: Download = Download()
    var setupKey: String = "AAAAAAAAAAAA"
    var restart: Restart = Restart()
}

class Download {
    var url: String = "https://github.com/netbirdio/netbird/releases/download/v{VERSION}/netbird_{VERSION}_linux_amd64.tar.gz"
    var version: String = "0.79.0"
    var cacheDir: String = ".cache/xtrust/netbird"
}

class Restart {
    var autoRestart: Boolean = true
    var restartAfter: Int = 30
}

