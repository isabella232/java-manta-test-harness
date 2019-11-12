# Java Manta Test Harness

[jav-manta-test-harness](https://github.com/nairashwin952013/java-manta-test-harness) is a community-maintained Java
testing app used for verifying Java 11 support by [java-manta](https://github.com/joyent/java-manta) client SDK for manta.

## Installation

[Java 11](http://www.oracle.com/technetwork/java/javase/downloads/index.html) is required. The main client module can
be installed by adding the following dependency using [Maven 3.5.x](https://maven.apache.org/):

### Using Maven
Add the latest java-manta-client dependency to your Maven `pom.xml`.

```
<dependency>
    <groupId>com.joyent.manta</groupId>
    <artifactId>java-manta-client</artifactId>
    <version>LATEST</version>
</dependency>
```

Consult [Maven Central search](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.joyent.manta%22%20AND%20a%3A%22java-manta-client%22)
for a list of all available versions and dependency strings for other project types.

Note: Users are expected to use the same version across sub-packages, e.g. using
`com.joyent.manta:java-manta-client:3.0.0` with
`com.joyent.manta:java-http-signature:4.0.10` is not supported.

#### Minimizing bundled dependencies

A separate artifact published as `java-manta-client-unshaded` can optionally be used in place of `java-manta-client` if
users want precise control over dependency resolution. This is only recommended for users comfortable with
debugging Maven dependency resolution and usage of [Maven Enforcer's Dependency Convergence
Rule](http://maven.apache.org/enforcer/enforcer-rules/dependencyConvergence.html) is *strongly* encouraged.

## From Source
If you prefer to build from source, you'll also need
[Maven](https://maven.apache.org/), and then invoke:

```
$ mvn clean insatll
```

If you want to skip running of the test suite use the `-DskipTests` property, e.g. `mvn -DskipTests package`.

## Configuration

Configuration can be done through system properties or environment variables.
Listed below are the most commonly used configuration options:

| System Property                    | Environment Variable           | Default                              |
|------------------------------------|--------------------------------|--------------------------------------|
| manta.url                          | MANTA_URL                      | https://us-east.manta.joyent.com:443 |
| manta.user                         | MANTA_USER                     |                                      |
| manta.key_id                       | MANTA_KEY_ID                   |                                      |
| manta.key_path                     | MANTA_KEY_PATH                 | $HOME/.ssh/id_rsa (if exists)        |
| manta.key_content                  | MANTA_KEY_CONTENT              |                                      |

* `manta.url` ( **MANTA_URL** )
The URL of the manta service endpoint to test against
* `manta.user` ( **MANTA_USER** )
The account name used to access the manta service. If accessing via a [subuser](https://docs.joyent.com/public-cloud/rbac/users),
you will specify the account name as "user/subuser".
* `manta.key_id`: ( **MANTA_KEY_ID**)
The fingerprint for the public key used to access the manta service. Can be obtained using `ssh-keygen -l -f ${MANTA_KEY_PATH} -E md5 | cut -d' ' -f 2`
* `manta.key_path` ( **MANTA_KEY_PATH**)
The name of the file that will be loaded for the account used to access the manta service. 
* `manta.key_content` ( **MANTA_KEY_CONTENT**)
The content of the private key as a string. This is an alternative to `manta.key_path`. Both
`manta.key_path` and can't be specified at the same time `manta.key_content`. This parameter can be specified 
independently since 3.4.1.

## Client-side Encryption

In order to enable client side encryption for downloading and decrypting encrypted files, please set the following
system properties. Please consult the [Configuration Parameters list](https://github.com/joyent/java-manta/blob/master/USAGE.md#parameters) 
for the corresponding environment variables.

- `manta.client_encryption` - set to `true`

Additionally, you should set the following system properties to support encrypting and uploading files using client-side
encryption.

- `manta.encryption_algorithm`
- `manta.encryption_key_id`

For more details see the information available here in the Java-Manta Documentation for [Client-Side Encryption](https://github.com/joyent/java-manta/blob/master/USAGE.md#client-side-encryption)

## Usage

- Execute a `mvn clean install` or `mvn clean verify` from the project's root directory to run all tests.
- A second approach will be to formulate a run configuration in the IDE you setup this project with and comprehensively
supply the mandatory configuration parameters like `MANTA_URL, MANTA_USER, MANTA_KEY_ID, MANTA_KEY_PATH` or 
`MANTA_KEY_CONTENT`.

## FAQs

Known edge cases and other topics are covered in [Java-Manta FAQ's](https://github.com/joyent/java-manta/blob/master/FAQ.md).

## Contributions

Contributions are welcome! Please read the [CONTRIBUTING.md](/CONTRIBUTING.md) document for details
on getting started.

### Bugs

See <https://github.com/joyent/java-manta/issues>.

## License
Java Manta Test Harness is licensed under the MPLv2. Please see the [LICENSE.txt](/LICENSE.txt)
file for more details. The license was changed from the MIT license to the MPLv2
license starting at version 2.3.0.

### Credits
We are grateful for the functionality provided by the libraries that this project
depends on. Without them, we would be building everything from scratch. A thank you
goes out to:

* [The Apache Commons Project](https://commons.apache.org/)
* [The Apache HTTP Components Project](http://hc.apache.org/)
* [The FastXML Project](https://github.com/FasterXML)
* [The Legion of the Bouncy Castle Project](https://www.bouncycastle.org/)
* [The SLF4J Project](http://www.slf4j.org/)
* [The JNAGMP Project](https://github.com/square/jna-gmp)
* [The TestNG Project](http://testng.org/doc/index.html)
* [The Mockito Project](http://site.mockito.org/)
* [Timothy W Macinta's FastMD5 Project](http://twmacinta.com/myjava/fast_md5.php)
* [Remko Popma's picocli Project](https://github.com/remkop/picocli)