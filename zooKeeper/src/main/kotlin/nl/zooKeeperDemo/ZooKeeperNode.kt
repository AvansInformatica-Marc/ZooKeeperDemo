package nl.zooKeeperDemo

import kotlinx.coroutines.Job

interface ZooKeeperNode {
    val path: String

    var data: ByteArray?

    var dataAsString: String?

    val children: List<ZooKeeperNode>

    suspend fun addChild(newPath: String, newData: ByteArray = byteArrayOf()): ZooKeeperNode

    suspend fun addChild(newPath: String, newData: String): ZooKeeperNode

    suspend fun delete(): Job

    val structure: String
}
