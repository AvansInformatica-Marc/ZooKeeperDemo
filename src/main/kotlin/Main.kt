import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

val currentDate: String = SimpleDateFormat("yyyy-MM-dd").format(Date())

class MyClass

suspend fun main(args: Array<String> = arrayOf()) {
    val host = when {
        args.contains("-h") -> args[args.indexOf("-h") + 1]
        args.contains("--host") -> args[args.indexOf("--host") + 1]
        else -> "localhost"
    }.removeSurrounding("\"")

    startZooKeeper(host) {
        val coronaDataNode = children.find {
            it.path.endsWith("corona-data")
        } ?: addChild("corona-data")

        println("Dates available: ${coronaDataNode.children.joinToString { it.path.substringAfterLast('/') }}")
        println("Type 'insert' to add new data, type a date to download data for that date:")

        val action = readLine()?.trim()?.toLowerCase()

        when {
            action.isNullOrBlank() -> println("Unknown action")
            action == "insert" -> writeNewCoronaData(coronaDataNode)
            else -> downloadCoronaData(action, coronaDataNode)
        }
    }
}

suspend inline fun downloadCoronaData(date: String, mainDataNode: ZooKeeper.Node) = coroutineScope {
    val dataNode = mainDataNode.children.find {
        it.path.endsWith("/$date")
    }

    if(dataNode != null) {
        val file = File("${System.getProperty("user.home")}${File.separatorChar}CoronaData", "$date.csv")
        val dirCreated = withContext(Dispatchers.IO) {
            Files.createDirectories(Paths.get(file.parentFile.path))
        } != null
        val fileCreated = dirCreated && withContext(Dispatchers.IO) { file.createNewFile() }

        val data = dataNode.data

        when {
            data == null || data.isEmpty() -> println("The data for date $date is empty")
            !dirCreated -> println("Can't create directory at ${file.parentFile.path}")
            !fileCreated -> println("Can't create file at ${file.path}")
            else -> {
                println("Writing data to ${file.path}")
                launch(Dispatchers.IO) {
                    file.writeBytes(data)
                }
            }
        }
    } else println("No data for date $date is found")
}

suspend inline fun writeNewCoronaData(dataNode: ZooKeeper.Node) = coroutineScope {
    val dataFileName = "corona-data-rivm_$currentDate.csv"
    val dataFileBytes = async(Dispatchers.IO) {
        MyClass().javaClass.getResourceAsStream(dataFileName)?.readAllBytes()
    }

    val previousData = dataNode.children.maxBy { it.path }
    val previousDataDate = previousData?.path?.substringAfterLast('/') ?: "N/A"

    if(dataNode.children.none { it.path.endsWith("/$currentDate") }) {
        println("Inserting data for $currentDate, previous data is from $previousDataDate")
        val data = dataFileBytes.await()
        if(data != null) dataNode.addChild(currentDate, data)
        else println("Can't find $dataFileName")
    }
}

suspend inline fun startZooKeeper(
    host: String = "localhost",
    crossinline block: suspend ZooKeeper.Instance.() -> Unit
) {
    println("Connecting to ZooKeeper instance at $host")

    withContext(Dispatchers.IO) {
        ZooKeeper.connect(host).use {
            block(it)
        }
    }
}
