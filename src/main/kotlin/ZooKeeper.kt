import kotlinx.coroutines.*
import org.apache.zookeeper.*
import org.apache.zookeeper.ZooKeeper as ApacheZooKeeper
import java.io.IOException
import java.util.concurrent.CountDownLatch

object ZooKeeper {
    open class Node internal constructor(internal val zooKeeper: ApacheZooKeeper, val path: String = "/") {
        var data: ByteArray?
            get() = zooKeeper.getData(path, null, null)
            set(value) {
                zooKeeper.setData(path, value ?: byteArrayOf(), zooKeeper.exists(path, true)?.version ?: 0)
            }

        var dataAsString: String?
            get() = data?.toString(Charsets.UTF_8)
            set(value) {
                data = value?.toByteArray(Charsets.UTF_8)
            }

        val children: List<Node>
            get() = zooKeeper.getChildren(path, null, null).map { Node(zooKeeper, path + if(it.startsWith("/") || path.endsWith("/")) it else "/$it") }

        suspend fun addChild(newPath: String, newData: ByteArray = byteArrayOf()) = withContext(Dispatchers.IO) {
            Node(zooKeeper, zooKeeper.create((if(path.endsWith("/")) path else "${path}/") + newPath, newData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT))
        }

        suspend fun addChild(newPath: String, newData: String)
                = addChild(newPath, newData.toByteArray(Charsets.UTF_8))

        open suspend fun delete(): Job = coroutineScope {
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
            return spaces + when {
                !hasData && !hasChildren -> path
                hasData && !hasChildren -> "$path = \"$dataAsString\""
                !hasData && hasChildren -> "$path: [\n${children.joinToString("\n") { it.getStructure(indent + 1) }}\n$spaces]"
                else -> "$path = \"$dataAsString\" : [\n${children.joinToString("\n") { it.getStructure(indent + 1) }}\n$spaces]"
            }
        }

        val structure
            get() = getStructure()
    }

    class Instance internal constructor(zooKeeper: ApacheZooKeeper) : Node(zooKeeper), AutoCloseable {
        override fun close() = zooKeeper.close()

        override suspend fun delete(): Job {
            throw IllegalStateException()
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    suspend fun connect(host: String = "localhost", timeout: Int = 2000) = withContext(Dispatchers.IO) {
        val connectionLatch = CountDownLatch(1)

        val instance = ApacheZooKeeper(host, timeout) {
            if(it.state == Watcher.Event.KeeperState.SyncConnected)
                connectionLatch.countDown()
        }

        connectionLatch.await()

        Instance(instance)
    }
}