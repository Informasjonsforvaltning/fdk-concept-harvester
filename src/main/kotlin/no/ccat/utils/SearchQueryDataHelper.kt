@file:JvmName("Utils")
@file:JvmMultifileClass
package no.ccat.utils


val nn = LanguageProperties("nn", "norwegian")
val en = LanguageProperties("en", "english")
val no = LanguageProperties("no", "norwegian")
val nb = LanguageProperties()

/**
 * @param langParam: language code from request
 * @return properties to be used for boosting with with elastichsearch,
 *          defaults to bokmål if language code is unknowm
 */
fun language(langParam: String) : LanguageProperties {
    return when (langParam) {
        nb.key -> nb
        nn.key -> nn
        en.key -> en
        no.key -> no
        else -> nb;
    }
}

/**
 * @param langParam: language code from request
 * @return properties to be used for boosting of exact matches with elastichsearch,
 *          defaults to bokmål if language code is unknowm
 */
fun secondaryLanguages(langParam: String ): Array<LanguageProperties>{
    return when (langParam) {
        nb.key -> arrayOf(nn,en,no)
        nn.key -> arrayOf(nb,en,no)
        en.key ->  arrayOf(nn,nb,no)
        no.key -> arrayOf(nn,en,nb)
        else -> arrayOf(nn,en,no)
    }
}

val allLanguages = listOf<String>("nb","nn","en","no")

data class LanguageProperties(var key : String = "nb", val analyzer: String = "norwegian", val stemmer: String? = null){

    init {
        if(key=="")
            key="nb"
    }

    fun secondaryKeys() : List<String> {
        val secondary : Array<LanguageProperties> = secondaryLanguages(key)

        val keyList = mutableListOf<String>();
        for (lang in secondary) {
            keyList.add(lang.key)
        }

        return keyList;
    }
}

data class QueryParams(val queryString: String = "",
                       val orgPath: String = "",
                       val lang: String = "",
                       val prefLabel: String="",
                       val startPage: String = "",
                       val size : String = "",
                       val returnFields: String = "",
                       val aggregation: String = "",
                       val sortField: String = "",
                       val sortDirection: String = ""
                       ){

    fun isEmpty() : Boolean{
        return this == QueryParams()
    }

    fun isEmptySearch() : Boolean{
        return queryString == "" && orgPath == "" && prefLabel == ""
    }

    fun isDefaultSize() : Boolean{
        return startPage == "" && size == ""
    }

    fun isDefaultPresentation() : Boolean{
        return  aggregation == "" && returnFields == "" && sortField == "" && sortDirection == ""
    }

    fun isPrefLabelSearch() : Boolean {
        return queryString == "" && prefLabel != ""
    }

}




