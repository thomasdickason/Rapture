/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.blob.mongodb;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.mongodb.MongoException;

import rapture.blob.BaseBlobStore;
import rapture.common.CallingContext;
import rapture.common.Messages;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;

public class MongoDBBlobStore extends BaseBlobStore {
    private final static String INSTANCE_NAME = "default";
    public static final String PRIMARY_CONFIG = "prefix";
    public static final String SECONDARY_CONFIG = "grid";
    public static final String MULTIPART = "multipart";
    private Messages mongoMsgCatalog = new Messages("Mongo");

    private BlobHandler blobHandler;

    @Override
    public Boolean storeBlob(CallingContext context, RaptureURI blobUri, Boolean append, InputStream content) {
        try {
            return blobHandler.storeBlob(context, blobUri.getDocPath(), content, append);
        } catch (MongoException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    mongoMsgCatalog.getMessage("WriteError", new String[] { blobUri.getDocPath(), e.getMessage() }));
        }
    }

    @Override
    public Boolean deleteBlob(CallingContext context, RaptureURI blobUri) {
        try {
            return blobHandler.deleteBlob(context, blobUri.getDocPath());
        } catch (MongoException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    mongoMsgCatalog.getMessage("DeleteError", new String[] { blobUri.getDocPath(), e.getMessage() }));
        }
    }

    @Override
    public Boolean deleteFolder(CallingContext context, RaptureURI blobUri) {
        return false; // not yet implemented
    }

    @Override
    public InputStream getBlob(CallingContext context, RaptureURI blobUri) {
        try {
            return blobHandler.getBlob(context, blobUri.getDocPath());
        } catch (MongoException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    mongoMsgCatalog.getMessage("ReadError", new String[] { blobUri.getDocPath(), e.getMessage() }));
        }

    }

    @Override
    public void setConfig(Map<String, String> config) {
        String bucket;
        if (config.containsKey(PRIMARY_CONFIG)) {
            bucket = config.get(PRIMARY_CONFIG);
        } else if (config.containsKey(SECONDARY_CONFIG)) {
            bucket = config.get(SECONDARY_CONFIG);
        } else {
            bucket = null;
        }
        if (StringUtils.isEmpty(bucket)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    mongoMsgCatalog.getMessage("PrefixOrGrid"));
        }

        if (Boolean.valueOf(config.get(MULTIPART))) {
            blobHandler = new DocHandler(INSTANCE_NAME, bucket);
        } else {
            blobHandler = new GridFSBlobHandler(INSTANCE_NAME, bucket);
        }
    }

    @Override
    public void init() {
        // TODO RAP-1582 but mongo
    }

    @Override
    public Boolean deleteRepo() {
        return true;
    }
}
