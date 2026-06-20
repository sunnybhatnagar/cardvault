package com.sunnyb.cardvault.util.coil

import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.sunnyb.cardvault.security.EncryptionManager
import okio.Buffer
import okio.BufferedSource
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.drawable.BitmapDrawable

class EncryptedImageFetcher(
    private val model: EncryptedImageModel,
    private val encryptionManager: EncryptionManager,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bitmap = encryptionManager.readEncryptedBitmap(model.filePath)
        return if (bitmap != null) {
            DrawableResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } else {
            null
        }
    }

    class Factory(private val encryptionManager: EncryptionManager) : Fetcher.Factory<EncryptedImageModel> {
        override fun create(data: EncryptedImageModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return EncryptedImageFetcher(data, encryptionManager, options)
        }
    }
}
