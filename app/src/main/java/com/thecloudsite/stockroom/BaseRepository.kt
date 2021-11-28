/*
 * Copyright (C) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.stockroom

import android.util.Log
import retrofit2.Response
import java.io.IOException

open class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

open class BaseRepository {

    suspend fun <T : Any> apiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: Result<T> = apiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is Result.Success ->
                data = result.data
            is Result.Error -> {
                Log.d(
                    "BaseRepository apiCall failed",
                    "$errorMessage , Exception: ${result.exception}"
                )
            }
        }

        return data
    }

    private suspend fun <T : Any> apiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): Result<T> {
        val response = call.invoke()
        if (response.isSuccessful) {
            return Result.Success(response.body()!!)
        }

        return Result.Error(
            IOException("Error in apiResult: $errorMessage")
        )
    }
}