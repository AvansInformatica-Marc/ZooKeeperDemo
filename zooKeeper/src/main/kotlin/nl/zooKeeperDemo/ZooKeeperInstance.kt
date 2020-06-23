package nl.zooKeeperDemo

interface ZooKeeperInstance : AutoCloseable {
    val host: String

    val rootNode: ZooKeeperNode
}
