package nl.zooKeeperDemo

suspend fun main(args: Array<String> = arrayOf()) {
    val host = when {
        args.contains("-h") -> args[args.indexOf("-h") + 1]
        args.contains("--host") -> args[args.indexOf("--host") + 1]
        else -> ZooKeeper.DEFAULT_HOST
    }.removeSurrounding("\"")

    println("Connecting to ZooKeeper instance at $host")

    ZooKeeper.connect(host) {
        CoronaApp.startApp(rootNode)
    }
}
