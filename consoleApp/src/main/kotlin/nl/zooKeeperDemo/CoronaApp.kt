package nl.zooKeeperDemo

import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

object CoronaApp {
    val currentDate: String = SimpleDateFormat("yyyy-MM-dd").format(Date())

    val appResources = AppResources()

    suspend inline fun startApp(mainNode: ZooKeeperNode) {
        val coronaDataNode = mainNode.children.find {
            it.path.endsWith("corona-data")
        } ?: mainNode.addChild("corona-data")

        loop@while(true) {
            println()
            println("---")
            println()

            println("Actions:")
            println("- list: shows a list of all dates that are available in the system")
            println("- read: read corona data")
            println("- download: downloads corona data to \"${System.getProperty("user.home")}${File.separatorChar}CoronaData\"")
            println("- insert: insert new corona data")
            println("- exit: exit this app")

            println()

            when(awaitInput()) {
                "list", "l" -> listDates(coronaDataNode)
                "insert", "i" -> writeNewCoronaData(coronaDataNode, askForDate())
                "read", "r" -> printCoronaData(coronaDataNode, askForDate())
                "download", "d" -> downloadCoronaData(coronaDataNode, askForDate())
                "exit", "quit", "q" -> break@loop
                else -> println("Unknown action, please try again")
            }
        }
    }

    fun listDates(coronaDataNode: ZooKeeperNode) {
        val dateNodes = coronaDataNode.children

        println(when {
            dateNodes.isEmpty() -> "No data has been inserted into the system yet."
            else -> "Data is available for dates: ${dateNodes.joinToString { it.path.substringAfterLast('/') }}"
        })
    }

    suspend inline fun printCoronaData(coronaDataNode: ZooKeeperNode, date: String = currentDate) {
        coroutineScope {
            val dataNode = coronaDataNode.children.find {
                it.path.endsWith("/$date")
            }

            when {
                dataNode == null -> println("No data for date $date is found")
                dataNode.dataAsString.isNullOrBlank() -> println("The data for date $date is empty")
                else -> println(dataNode.dataAsString)
            }
        }
    }

    suspend inline fun downloadCoronaData(coronaDataNode: ZooKeeperNode, date: String = currentDate) {
        coroutineScope {
            val dataNode = coronaDataNode.children.find {
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
    }

    fun awaitInput(): String? {
        print("> ")
        val input = readLine()?.trim()?.toLowerCase()
        println()
        return input
    }

    fun askForDate(): String {
        println("Enter a date (yyyy-MM-dd), leave empty to use the current date ($currentDate): ")
        val enteredDate = awaitInput()
        return when {
            enteredDate.isNullOrBlank() -> currentDate
            else -> enteredDate
        }
    }

    suspend inline fun writeNewCoronaData(coronaDataNode: ZooKeeperNode, date: String = currentDate) = coroutineScope {
        val dataFileName = "corona-data-rivm_$date.csv"
        val dataFileBytes = async(Dispatchers.IO) {
            appResources[dataFileName]?.readAllBytes()
        }

        val previousData = coronaDataNode.children.maxBy { it.path }
        val previousDataDate = previousData?.path?.substringAfterLast('/') ?: "N/A"

        if(coronaDataNode.children.none { it.path.endsWith("/$date") }) {
            println("Inserting data for $date, previous data is from $previousDataDate")
            val data = dataFileBytes.await()
            if(data != null) coronaDataNode.addChild(date, data)
            else println("Can't find $dataFileName")
        }
    }
}
