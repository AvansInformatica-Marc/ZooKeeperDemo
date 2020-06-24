package nl.zooKeeperDemo

import kotlinx.coroutines.*
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooDefs
import java.io.IOException
import java.util.concurrent.CountDownLatch
import org.apache.zookeeper.ZooKeeper as ApacheZooKeeper

object ZooKeeper {
    const val DEFAULT_TIMEOUT = 2000

    const val DEFAULT_HOST = "localhost"

    internal class Node(private val zooKeeper: ApacheZooKeeper, override val path: String = "/") : ZooKeeperNode {
        override var data: ByteArray?
            get() = zooKeeper.getData(path, null, null)
            set(value) {
                zooKeeper.setData(path, value ?: byteArrayOf(), zooKeeper.exists(path, true)?.version ?: 0)
            }

        override var dataAsString: String?
            get() = data?.toString(Charsets.UTF_8)
            set(value) {
                data = value?.toByteArray(Charsets.UTF_8)
            }

        override val children: List<Node>
            get() = zooKeeper.getChildren(path, null, null).map {
                Node(
                    zooKeeper,
                    path + if (it.startsWith("/") || path.endsWith("/")) it else "/$it"
                )
            }

        override suspend fun addChild(newPath: String, newData: ByteArray) = withContext(Dispatchers.IO) {
            Node(
                zooKeeper,
                zooKeeper.create(
                    (if (path.endsWith("/")) path else "${path}/") + newPath,
                    newData,
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
                )
            )
        }

        override suspend fun addChild(newPath: String, newData: String)
                = addChild(newPath, newData.toByteArray(Charsets.UTF_8))

        override suspend fun delete(): Job = coroutineScope {
            launch(Dispatchers.IO) {
                for(child in children)
                    child.delete()

                zooKeeper.delete(path, zooKeeper.exists(path, true)?.version ?: 0)
            }
        }

        override fun toString() = path

        private fun getStructure(indent: Int = 0): String {
            val spaces = " ".repeat(indent * 2)
            val hasData = !dataAsString.isNullOrBlank()
            val hasChildren = children.isNotEmpty()

            fun getChildrenAsString(): String {
                val childrenAsString = children.joinToString("\n") { it.getStructure(indent + 1) }
                return "[\n$childrenAsString\n$spaces]"
            }

            return spaces + when {
                !hasData && !hasChildren -> path
                hasData && !hasChildren -> "$path = \"$dataAsString\""
                !hasData && hasChildren -> "$path: ${getChildrenAsString()}"
                else -> "$path = \"$dataAsString\" : ${getChildrenAsString()}"
            }
        }

        override val structure
            get() = getStructure()
    }

    internal class Instance(private val zooKeeper: ApacheZooKeeper, override val host: String) : ZooKeeperInstance {
        override val rootNode = Node(zooKeeper)

        override fun close() = zooKeeper.close()
    }

    @Throws(IOException::class, InterruptedException::class)
    suspend fun connect(
        host: String = DEFAULT_HOST,
        timeout: Int = DEFAULT_TIMEOUT
    ): ZooKeeperInstance = withContext(Dispatchers.IO) {
        val connectionLatch = CountDownLatch(1)

        val instance = ApacheZooKeeper(host, timeout) {
            if(it.state == Watcher.Event.KeeperState.SyncConnected)
                connectionLatch.countDown()
        }

        connectionLatch.await()

        Instance(instance, host)
    }

    suspend inline fun connect(
        host: String = DEFAULT_HOST,
        timeout: Int = DEFAULT_TIMEOUT,
        crossinline block: suspend ZooKeeperInstance.() -> Unit
    ) = withContext(Dispatchers.IO) {
        connect(host, timeout).use {
            block(it)
        }
    }
}
