/*
 * Copyright (c) 2016-2019, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.client;

import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.IntegrationTestConfigContext;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.test.util.MantaAssert;
import com.joyent.test.util.MantaFunction;
import com.joyent.test.util.RandomInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

import static com.joyent.manta.exception.MantaErrorCode.RESOURCE_NOT_FOUND_ERROR;

/**
 * Tests the basic functionality of the put operations in {@link MantaClient} class.
 */
@Test(groups = {"put"})
public class MantaClientPutIT {

    private static final String TEST_DATA = "EPISODEII_IS_BEST_EPISODE";
    private static final String TEST_FILENAME = "Master-Yoda.jpg";

    private MantaClient mantaClient;

    private String testPathPrefix;
    private Boolean usingEncryption;

    @BeforeClass
    @Parameters({"usingEncryption"})
    public void beforeClass(@Optional Boolean usingEncryption) throws IOException {
        this.usingEncryption = usingEncryption;
        // Let TestNG configuration take precedence over environment variables
        ConfigContext config = new IntegrationTestConfigContext(usingEncryption);

        mantaClient = new MantaClient(config);
        testPathPrefix = IntegrationTestConfigContext.generateBasePath(config, this.getClass().getSimpleName());
        mantaClient.putDirectory(testPathPrefix, true);
    }

    @AfterClass
    public void afterClass() throws IOException {
        IntegrationTestConfigContext.cleanupTestDirectory(mantaClient, testPathPrefix);
    }

    @Test
    public final void testPutWithStringUTF8() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;

        MantaObject response = mantaClient.put(path, TEST_DATA, StandardCharsets.UTF_8);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "text/plain; charset=UTF-8",
                "Content type wasn't detected correctly");

        try (MantaObjectInputStream object = mantaClient.getAsInputStream(path)) {
            String actual = IOUtils.toString(object, StandardCharsets.UTF_8);

            Assert.assertEquals(actual, TEST_DATA,
                    "Uploaded string didn't match expectation");
        }
    }

    @Test
    public final void testPutWithErrorProneCharacters() throws IOException {
        final String name = UUID.randomUUID().toString() + "- -`~!@#$%^&*().txt";
        final String path = testPathPrefix + name;

        MantaObject response = mantaClient.put(path, TEST_DATA, StandardCharsets.UTF_8);
        try (MantaObjectInputStream object = mantaClient.getAsInputStream(path)) {
            String actual = IOUtils.toString(object, StandardCharsets.UTF_8);

            Assert.assertEquals(actual, TEST_DATA,
                    "Uploaded string didn't match expectation");
            Assert.assertEquals(response.getPath(), path, "path not returned as written");
            Assert.assertEquals(object.getPath(), path, "path not returned as written");
        }
    }

    @Test
    public final void testPutWithStringUTF16() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;

        MantaObject response = mantaClient.put(path, TEST_DATA, StandardCharsets.UTF_16);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "text/plain; charset=UTF-16",
                "Content type wasn't detected correctly");

        try (MantaObjectInputStream object = mantaClient.getAsInputStream(path)) {
            String actual = IOUtils.toString(object, StandardCharsets.UTF_16);

            Assert.assertEquals(actual, TEST_DATA,
                    "Uploaded string didn't match expectation");
        }
    }

    @Test
    public final void testPutWithStreamThatDoesntFitInBuffer() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Assert.assertNotNull(classLoader.getResource(TEST_FILENAME));

        final int length = mantaClient.getContext().getUploadBufferSize() + 1024;

        try (InputStream testDataInputStream = new RandomInputStream(length)) {
            Assert.assertFalse(testDataInputStream.markSupported());
            mantaClient.put(path, testDataInputStream);
        }
    }

    @Test
    public final void testPutWithStreamAndKnownContentLength() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Assert.assertNotNull(classLoader.getResource(TEST_FILENAME));

        final int length = 20 * 1024;
        try (InputStream testDataInputStream = new RandomInputStream(length)) {
            mantaClient.put(path, testDataInputStream, length, null, null);
        }
    }

    @Test
    public final void testPutWithStreamAndKnownContentLengthBeyondBuffer() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Assert.assertNotNull(classLoader.getResource(TEST_FILENAME));

        final int length = mantaClient.getContext().getUploadBufferSize() + 1024;
        try (InputStream testDataInputStream = new RandomInputStream(length)) {
            mantaClient.put(path, testDataInputStream, length, null, null);
        }
    }

    @Test
    public final void testPutWithStreamAndErrorProneName() throws IOException {
        final String name = UUID.randomUUID().toString() + "- -!@#$%^&*().txt";
        final String path = testPathPrefix + name;
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Assert.assertNotNull(classLoader.getResource(TEST_FILENAME));

        final int length = mantaClient.getContext().getUploadBufferSize() + 1024;

        try (InputStream testDataInputStream = new RandomInputStream(length)) {
            Assert.assertFalse(testDataInputStream.markSupported());
            mantaClient.put(path, testDataInputStream);
        }
        try (MantaObjectInputStream object = mantaClient.getAsInputStream(path)) {
            Assert.assertEquals(object.getPath(), path, "path not returned as written");
            byte[] actualBytes = IOUtils.readFully(object, length);
            Assert.assertTrue(actualBytes.length > 0);
        }
    }

    @Test
    public final void testPutWithByteArray() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        final byte[] content = TEST_DATA.getBytes(StandardCharsets.UTF_8);

        MantaObject response = mantaClient.put(path, content);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "application/octet-stream",
                "Content type wasn't set to expected default");

        final String actual = mantaClient.getAsString(path, StandardCharsets.UTF_8);

        Assert.assertEquals(actual, TEST_DATA,
                "Uploaded byte array was malformed");
    }

    @Test
    public final void testPutWithByteArrayAndErrorProneCharacters() throws IOException {
        final String name = UUID.randomUUID().toString() + "- -!@#$%^&*().bin";
        final String path = testPathPrefix + name;
        final byte[] content = TEST_DATA.getBytes(StandardCharsets.UTF_8);

        MantaObject response = mantaClient.put(path, content);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "application/octet-stream",
                "Content type wasn't set to expected default");

        final String actual = mantaClient.getAsString(path, StandardCharsets.UTF_8);

        Assert.assertEquals(actual, TEST_DATA,
                "Uploaded byte array was malformed");
    }

    @Test
    public final void testPutWithByteArrayAndContentType() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        final byte[] content = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String contentType = "text/plain; charset=UTF-8";

        MantaHttpHeaders headers = new MantaHttpHeaders();
        headers.setContentType(contentType);
        MantaObject response = mantaClient.put(path, content, headers);

        Assert.assertEquals(response.getContentType(), contentType,
                "Content type wasn't set to expected default");

        try (MantaObjectInputStream in = mantaClient.getAsInputStream(path)) {
            final String actual = IOUtils.toString(in, StandardCharsets.UTF_8);
            Assert.assertEquals(actual, TEST_DATA,
                    "Uploaded byte array was malformed");
            Assert.assertEquals(in.getContentType(), contentType,
                    "Content type wasn't set to expected default");
        }

        if (BooleanUtils.isTrue(usingEncryption)) {
            try (MantaClient unencryptedClient = new MantaClient(new IntegrationTestConfigContext(false))) {
                MantaObjectResponse unencryptedResponse = unencryptedClient.head(path);
                Assert.assertEquals(unencryptedResponse.getContentType(),
                        "application/octet-stream",
                        "Plaintext content type was leaked");
            }
        }
    }

    @Test
    public final void testPutWithPlainTextFileUTF8RomanCharacters() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        File temp = File.createTempFile("upload", ".txt");
        FileUtils.forceDeleteOnExit(temp);

        Files.write(temp.toPath(), TEST_DATA.getBytes(StandardCharsets.UTF_8));
        MantaObject response = mantaClient.put(path, temp);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "text/plain",
                "Content type wasn't detected correctly");

        String actual = mantaClient.getAsString(path);
        Assert.assertEquals(actual, TEST_DATA,
                "Uploaded file didn't match expectation");
    }

    @Test
    public final void testPutWithPlainTextFileUTF8NonRomanCharacters() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        File temp = File.createTempFile("upload", ".txt");
        FileUtils.forceDeleteOnExit(temp);

        final String content = "これは日本語です。";

        Files.write(temp.toPath(), content.getBytes(StandardCharsets.UTF_8));
        MantaObject response = mantaClient.put(path, temp);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "text/plain",
                "Content type wasn't detected correctly");

        String actual = mantaClient.getAsString(path);
        Assert.assertEquals(actual, content,
                "Uploaded file didn't match expectation");
    }

    @Test
    public final void testPutWithPlainTextFileWithErrorProneName() throws IOException {
        final String name = UUID.randomUUID().toString() + "- -~!@#$%^&*().txt";
        final String path = testPathPrefix + name;
        File temp = File.createTempFile("upload", ".txt");
        FileUtils.forceDeleteOnExit(temp);

        Files.write(temp.toPath(), TEST_DATA.getBytes(StandardCharsets.UTF_8));
        MantaObject response = mantaClient.put(path, temp);
        String contentType = response.getContentType();
        Assert.assertEquals(contentType, "text/plain",
                "Content type wasn't detected correctly");

        String actual = mantaClient.getAsString(path);
        Assert.assertEquals(actual, TEST_DATA,
                "Uploaded file didn't match expectation");
        Assert.assertEquals(response.getPath(), path, "path returned as written");
    }

    @Test
    public final void testPutWithJPGFile() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        File temp = File.createTempFile("upload", ".jpg");
        FileUtils.forceDeleteOnExit(temp);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream testDataInputStream = classLoader.getResourceAsStream(TEST_FILENAME);
             OutputStream out = new FileOutputStream(temp)) {
            IOUtils.copy(testDataInputStream, out);
            MantaObject response = mantaClient.put(path, temp);
            String contentType = response.getContentType();
            Assert.assertEquals(contentType, "image/jpeg",
                    "Content type wasn't detected correctly");

            try (InputStream in = mantaClient.getAsInputStream(path)) {
                byte[] actual = IOUtils.toByteArray(in);
                byte[] expected = FileUtils.readFileToByteArray(temp);

                Assert.assertTrue(Arrays.equals(actual, expected),
                        "Uploaded file isn't the same as actual file");
            }
        }
    }

    @Test
    public final void testPutWithFileInputStreamAndNoContentLength() throws IOException {
        final String name = UUID.randomUUID().toString();
        final String path = testPathPrefix + name;
        File temp = Files.createTempFile("name", ".data").toFile();
        FileUtils.forceDeleteOnExit(temp);
        FileUtils.writeStringToFile(temp, TEST_DATA, StandardCharsets.UTF_8);

        // Test putting with an unknown content length
        try (FileInputStream in = new FileInputStream(temp)){
            mantaClient.put(path, in);
        }

        try (final MantaObjectInputStream gotObject = mantaClient.getAsInputStream(path)) {
            Assert.assertNotNull(gotObject);
            Assert.assertNotNull(gotObject.getContentType());
            Assert.assertNotNull(gotObject.getContentLength());
            Assert.assertNotNull(gotObject.getEtag());
            Assert.assertNotNull(gotObject.getMtime());
            Assert.assertNotNull(gotObject.getPath());

            final String data = IOUtils.toString(gotObject, Charset.defaultCharset());
            Assert.assertEquals(data, TEST_DATA);
        }

        mantaClient.delete(path);

        MantaAssert.assertResponseFailureStatusCode(404, RESOURCE_NOT_FOUND_ERROR,
                (MantaFunction<Object>) () -> mantaClient.get(testPathPrefix + name));
    }
}
