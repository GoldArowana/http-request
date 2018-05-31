package com.king.http.request.lib;

/**
 * Callback interface for reporting upload progress for a request.
 */
public interface UploadProgress {
    UploadProgress DEFAULT = new UploadProgress() {
        public void onUpload(long uploaded, long total) {
        }
    };

    /**
     * Callback invoked as data is uploaded by the request.
     *
     * @param uploaded The number of bytes already uploaded
     * @param total    The total number of bytes that will be uploaded or -1 if
     *                 the length is unknown.
     */
    void onUpload(long uploaded, long total);
}
