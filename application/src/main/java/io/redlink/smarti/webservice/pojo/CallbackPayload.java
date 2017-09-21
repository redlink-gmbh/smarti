/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.redlink.smarti.webservice.pojo;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CallbackPayload<T> {

    public enum Result {
        success,
        error;
    }

    private final Result result;
    private final T data;

    public CallbackPayload(T data) {
        this(Result.success, data);
    }

    public CallbackPayload(Result result, T data) {
        this.result = result;
        this.data = data;
    }

    public Result getResult() {
        return result;
    }

    public T getData() {
        return data;
    }
}
