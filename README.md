# shardingsphere-presto-local-transaction-test

- For https://github.com/apache/shardingsphere/pull/35504 .
- Execute the following command on the `Windows 11 Home 24H2` instance with `PowerShell/PowerShell`,
  `version-fox/vfox`, `git-for-windows/git` and `rancher-sandbox/rancher-desktop` installed.

```shell
vfox add java
vfox install java@21.0.7-ms
vfox use --global java@21.0.7-ms

git clone git@github.com:apache/shardingsphere.git
cd ./shardingsphere/
git reset --hard a623b4d54aae9ced5accc5fed1f7939211c39691
./mvnw clean install -Prelease -T1C -DskipTests "-Djacoco.skip=true" "-Dcheckstyle.skip=true" "-Drat.skip=true" "-Dmaven.javadoc.skip=true"
cd ../

git clone git@github.com:linghengqian/shardingsphere-presto-local-transaction-test.git
cd ./shardingsphere-presto-local-transaction-test/
./mvnw clean test -T 1C -Dtest=ShardingSphereTest
```

- The log is as follows.

```shell
PS D:\TwinklingLiftWorks\git\public\shardingsphere-presto-local-transaction-test> ./mvnw clean test -T 1C -Dtest=ShardingSphereTest
[INFO] Scanning for projects...
[INFO] 
[INFO] Using the MultiThreadedBuilder implementation with a thread count of 16
[INFO] 
[INFO] --< com.github.linghengqian:shardingsphere-presto-local-transaction-test >--
[INFO] Building shardingsphere-presto-local-transaction-test 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.2.0:clean (default-clean) @ shardingsphere-presto-local-transaction-test ---
[INFO] Deleting D:\TwinklingLiftWorks\git\public\shardingsphere-presto-local-transaction-test\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ shardingsphere-presto-local-transaction-test ---
[INFO] skip non existing resourceDirectory D:\TwinklingLiftWorks\git\public\shardingsphere-presto-local-transaction-test\src\main\resources
[INFO]
[INFO] --- compiler:3.13.0:compile (default-compile) @ shardingsphere-presto-local-transaction-test ---
[INFO] No sources to compile
[INFO]
[INFO] --- resources:3.3.1:testResources (default-testResources) @ shardingsphere-presto-local-transaction-test ---
[INFO] Copying 3 resources from src\test\resources to target\test-classes
[INFO]
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ shardingsphere-presto-local-transaction-test ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 1 source file with javac [debug release 21] to target\test-classes
[INFO] 
[INFO] --- surefire:3.2.5:test (default-test) @ shardingsphere-presto-local-transaction-test ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.github.linghengqian.ShardingSphereTest
5月 26, 2025 10:53:31 下午 org.junit.jupiter.engine.descriptor.AbstractExtensionContext lambda$createCloseAction$1
警告: Type implements CloseableResource but not AutoCloseable: org.testcontainers.junit.jupiter.TestcontainersExtension$StoreAdapter
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 19.35 s <<< FAILURE! -- in com.github.linghengqian.ShardingSphereTest
[ERROR] com.github.linghengqian.ShardingSphereTest.assertShardingInLocalTransactions -- Time elapsed: 19.16 s <<< FAILURE!
java.lang.AssertionError:

Expected: is <true>
     but: was <false>
        at org.hamcrest.MatcherAssert.assertThat(MatcherAssert.java:18)
        at org.hamcrest.MatcherAssert.assertThat(MatcherAssert.java:6)
        at com.github.linghengqian.ShardingSphereTest.assertRollbackWithTransactions(ShardingSphereTest.java:118)
        at com.github.linghengqian.ShardingSphereTest.assertShardingInLocalTransactions(ShardingSphereTest.java:44)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1597)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1597)

[INFO] 
[INFO] Results:
[INFO]
[ERROR] Failures: 
[ERROR]   ShardingSphereTest.assertShardingInLocalTransactions:44->assertRollbackWithTransactions:118 
Expected: is <true>
     but: was <false>
[INFO]
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  23.427 s (Wall Clock)
[INFO] Finished at: 2025-05-26T22:53:32+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test (default-test) on project shardingsphere-presto-local-transaction-test: There are test failures.
[ERROR]
[ERROR] Please refer to D:\TwinklingLiftWorks\git\public\shardingsphere-presto-local-transaction-test\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
```
