@file:JvmName("Utils")
@file:JvmMultifileClass
package no.ccat.utils


data class LanguageProperties(val key : String = "nb", val analyzer: String = "norwegian", val stemmer: String? = null)

data class LanguageUtils(val nb : LanguageProperties = LanguageProperties()){
    private val nn = LanguageProperties("nn", "norwegian")
    private val en = LanguageProperties("en", "english")

    /**
     * @param langParam: language code from request
     * @return properties to be used for boosting with with elastichsearch,
     *          defaults to bokmål if language code is unknowm
     */
    fun getLanguage(langParam: String) : LanguageProperties {
        return when (langParam) {
            nb.key -> nb
            nn.key -> nn
            en.key -> en
            else -> nb;
        }
    }

    /**
     * @param langParam: language code from request
     * @return properties to be used for boosting of exact matches with elastichsearch,
     *          defaults to bokmål if language code is unknowm
     */
    fun getSecondaryLanguage(langParam: String ): Array<LanguageProperties>{
        return when (langParam) {
            nb.key -> arrayOf(nn,en)
            nn.key -> arrayOf(nb,en)
            en.key ->  arrayOf(nn,nb)
            else -> arrayOf(nn,en)
        }
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




