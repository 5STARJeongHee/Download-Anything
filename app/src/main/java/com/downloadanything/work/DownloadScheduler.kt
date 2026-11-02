// 다운로드 작업을 WorkManager에 등록한다
package com.downloadanything.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.downloadanything.domain.model.DownloadRequest
import java.util.UUID

object DownloadScheduler {

    const val TAG = "download"

    fun enqueue(context: Context, request: DownloadRequest): UUID {
        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(Data.Builder().putAll(DownloadRequestCodec.encode(request)).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueue(work)
        return work.id
    }
}
