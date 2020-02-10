package testUtils

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext

val jsonPathParser = JsonPath
        .using(Configuration.builder()
                .options(Option.AS_PATH_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build())

val jsonValueParser = JsonPath
        .using(Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST)
                .build())

