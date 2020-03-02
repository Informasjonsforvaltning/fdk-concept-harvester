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
                       val sortDirection: String = "",
                       val uris: Set<String>? = emptySet(),
                       val identifiers: Set<String>? = emptySet()
                       ){

    val queryType: QueryType

    init {
        queryType = when {
            isQueryStringSearch() && !shouldfilterOnOrgPath() -> QueryType.queryStringSearch
            isQueryStringSearch() && shouldfilterOnOrgPath() -> QueryType.queryStringSearchWithOrgPath
            isPrefLabelSearch() && !shouldfilterOnOrgPath() -> QueryType.prefLabelSearch
            isPrefLabelSearch() && shouldfilterOnOrgPath() -> QueryType.prefLabelSearcgWithOrgPath
            isUriSearch() && !isTextSearch() && !isIdentifiersSearch() -> QueryType.urisSearch
            isIdentifiersSearch() && !isTextSearch() && !isUriSearch() -> QueryType.identifiersSearch
            else -> QueryType.matchAllSearch
        }
    }

    fun isEmpty() = this == QueryParams()

    fun isEmptySearchQuery() = queryString == "" && orgPath == "" && prefLabel == "" && uris.isNullOrEmpty() && identifiers.isNullOrEmpty()

    fun isDefaultSize() = startPage == "" && size == ""

    fun isDefaultPresentation() = aggregation == "" && returnFields == "" && sortField == "" && sortDirection == ""

    fun isPrefLabelSearch() = queryString == "" && prefLabel != ""

    fun isUriSearch() = !uris.isNullOrEmpty() && !isTextSearch()

    fun isIdentifiersSearch() = !identifiers.isNullOrEmpty() && !isTextSearch()

    fun isTextSearch() = isPrefLabelSearch() || isQueryStringSearch()

    fun isIdSearch() = isUriSearch() || isIdentifiersSearch()

    //Defaults to textserach if more than one search query value is present
    fun isQueryStringSearch() = queryString != ""

    fun shouldfilterOnOrgPath()= orgPath != ""

}

enum class QueryType{
    queryStringSearch,
    queryStringSearchWithOrgPath,
    prefLabelSearch,
    prefLabelSearcgWithOrgPath,
    urisSearch,
    identifiersSearch,
    matchAllSearch
}




