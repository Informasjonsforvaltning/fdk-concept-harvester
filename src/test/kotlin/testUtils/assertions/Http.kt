package testUtils.assertions

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import org.springframework.http.HttpStatus

import testUtils.getApiAddress
import testUtils.getElasticAddress

fun apiGet(endpoint: String): String {

    return try{
        val connection = URL(getApiAddress(endpoint))
                .openConnection() as HttpURLConnection

        connection.connect()
        if(isOK(connection.responseCode)) {
            connection.getInputStream().bufferedReader().use(BufferedReader::readText)
        } else {
            "error"
        }

    } catch (e: Exception){
        "error"
    }
}

fun isOK(response: Int?): Boolean =
        if(response == null) false
        else HttpStatus.resolve(response)?.is2xxSuccessful == true