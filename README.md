[![JVM CI with Gradle](https://github.com/AvansInformatica-Marc/ZooKeeperDemo/workflows/JVM%20CI%20with%20Gradle/badge.svg)](https://github.com/AvansInformatica-Marc/ZooKeeperDemo/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=AvansInformatica-Marc_ZooKeeperDemo&metric=alert_status)](https://sonarcloud.io/dashboard?id=AvansInformatica-Marc_ZooKeeperDemo)
[![License: MIT](https://badgen.net/badge/license/MIT/blue)](https://github.com/AvansInformatica-Marc/ZooKeeperDemo/blob/main/LICENSE)
# ZooKeeperDemo
Demo that stores and retrieves data (in this case reported COVID-19 cases in The Netherlands per municipality, [from RIVM](https://www.rivm.nl/)) using ZooKeeper.

## Usage
Import this project into a **recent version** of the IntelliJ IDEA (like 2020.1.2) with Kotlin plugin (stable version 1.3.72 or higher).

Run this project using the green arrow in consoleApp/src/main/kotlin/nl/zooKeeperDemo/Main.kt.

To modify the host, add program argument `--host "example.com"` or `-h "example.com"` in the run/debug configuration window.
