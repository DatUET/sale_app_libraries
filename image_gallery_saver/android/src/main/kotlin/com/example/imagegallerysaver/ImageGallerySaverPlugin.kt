isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH, when {
                        isVideo -> Environment.DIRECTORY_MOVIES
                        else -> Environment.DIRECTORY_PICTURES
                    }
                )
                if (!TextUtils.isEmpty(mimeType)) {
                    put(when {isVideo -> MediaStore.Video.Media.MIME_TYPE
                        else -> MediaStore.Images.Media.MIME_TYPE
                    }, mimeType)
                }
            }

            applicationContext?.contentResolver?.insert(uri, values)

        } else {
            // < android 10
            val storePath =
                Environment.getExternalStoragePublicDirectory(when {
                    isVideo -> Environment.DIRECTORY_MOVIES
                    else -> Environment.DIRECTORY_PICTURES
                }).absolutePath
            val appDir = File(storePath).apply {
                if (!exists()) {
                    mkdir()
                }
            }

            val file =
                File(appDir, if (extension.isNotEmpty()) "$fileName.$extension" else fileName)
            Uri.fromFile(file)
        }
    }

    /**
     * get file Mime Type
     *
     * @param extension extension
     * @return file Mime Type
     */
    private fun getMIMEType(extension: String): String? {
        return if (!TextUtils.isEmpty(extension)) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            null
        }
    }

    /**
     * Send storage success notification
     *
     * @param context context
     * @param fileUri file path
     */
    private fun sendBroadcast(context: Context, fileUri: Uri?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = fileUri
            context.sendBroadcast(mediaScanIntent)
        }
    }

    private fun saveImageToGallery(
        bmp: Bitmap?,
        quality: Int?,
        name: String?
    ): HashMap<String, Any?> {
        // check parameters
        if (bmp == null || quality == null) {
            return SaveResultModel(false, null, "parameters error").toHashMap()
        }
        // check applicationContext
        val context = applicationContext
            ?: return SaveResultModel(false, null, "applicationContext null").toHashMap()
        var fileUri: Uri? = null
        var fos: OutputStream? = null
        var success = false
        try {
            fileUri = generateUri("jpg", name = name)
            if (fileUri != null) {
                fos = context.contentResolver.openOutputStream(fileUri)
