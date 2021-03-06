package nl.zooKeeperDemo

import java.io.File
import java.io.InputStream

class AppResources {
    operator fun get(fileName: String): InputStream? {
        return javaClass.getResourceAsStream(fileName) ?: File("./consoleApp/src/main/resources/$fileName").inputStream()
    }
}
