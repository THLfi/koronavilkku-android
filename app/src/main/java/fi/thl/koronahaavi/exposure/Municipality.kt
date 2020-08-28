package fi.thl.koronahaavi.exposure

import fi.thl.koronahaavi.data.OmaoloFeatures

data class Municipality (
    val code: String,
    val name: String,
    val omaoloFeatures: OmaoloFeatures,
    val contacts: List<MunicipalityContact>
)

data class MunicipalityContact (
    val title: String,
    val phoneNumber: String,
    val info: String?
)
