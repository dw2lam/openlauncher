package com.openlauncher.app.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// --- VALHALLA API MODELS ---

data class ValhallaLocation(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("type") val type: String = "break"
)

data class AutoCostingOptions(
    @SerializedName("country_crossing_penalty") val countryCrossingPenalty: Double = 2000.0
)

data class CostingOptions(
    @SerializedName("auto") val auto: AutoCostingOptions = AutoCostingOptions()
)

data class DirectionsOptions(
    @SerializedName("units") val units: String = "kilometers",
    @SerializedName("language") val language: String = "en-US"
)

data class ValhallaRequest(
    @SerializedName("locations") val locations: List<ValhallaLocation>,
    @SerializedName("costing") val costing: String = "auto",
    @SerializedName("costing_options") val costingOptions: CostingOptions = CostingOptions(),
    @SerializedName("directions_options") val directionsOptions: DirectionsOptions = DirectionsOptions()
)

data class ValhallaResponse(
    @SerializedName("trip") val trip: ValhallaTrip?
)

data class ValhallaTrip(
    @SerializedName("summary") val summary: ValhallaSummary?,
    @SerializedName("legs") val legs: List<ValhallaLeg>?
)

data class ValhallaSummary(
    @SerializedName("time") val time: Double, // in seconds
    @SerializedName("length") val length: Double, // in kilometers/miles
    @SerializedName("min_lat") val minLat: Double,
    @SerializedName("min_lon") val minLon: Double,
    @SerializedName("max_lat") val maxLat: Double,
    @SerializedName("max_lon") val maxLon: Double
)

data class ValhallaLeg(
    @SerializedName("summary") val summary: ValhallaSummary?,
    @SerializedName("shape") val shape: String?,
    @SerializedName("maneuvers") val maneuvers: List<ValhallaManeuver>?
)

data class ValhallaManeuver(
    @SerializedName("type") val type: Int,
    @SerializedName("instruction") val instruction: String,
    @SerializedName("verbal_transition_alert_instruction") val verbalInstruction: String?,
    @SerializedName("distance") val distance: Double, // in km/miles
    @SerializedName("time") val time: Double, // in seconds
    @SerializedName("begin_shape_index") val beginShapeIndex: Int,
    @SerializedName("end_shape_index") val endShapeIndex: Int
)

// --- NOMINATIM GEOCODING MODELS ---

data class NominatimPlace(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String
)

// --- RETROFIT INTERFACES ---

interface ValhallaApiService {
    @POST("route")
    suspend fun getRoute(@Body request: ValhallaRequest): ValhallaResponse
}

interface NominatimApiService {
    @GET("search")
    suspend fun searchPlace(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("accept-language") acceptLanguage: String = "es"
    ): List<NominatimPlace>
}

// --- API CLIENT OBJECTS ---

object ValhallaApi {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("X-Client-Id", "openlauncher-android-valhalla")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: ValhallaApiService = Retrofit.Builder()
        .baseUrl("https://valhalla1.openstreetmap.de/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ValhallaApiService::class.java)
}

object NominatimApi {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "OpenLauncherAndroid/1.0 (victor@example.com)") // Nominatim requires User-Agent
                .build()
            chain.proceed(request)
        }
        .build()

    val service: NominatimApiService = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NominatimApiService::class.java)
}

// --- DECODER UTIL ---

object ValhallaPolylineDecoder {
    /**
     * Decodes a Valhalla 6-decimal-precision polyline shape.
     */
    fun decode(encoded: String): List<GeoPoint> {
        val poly = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result valhrsh 1).inv() else result valhrsh 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result valhrsh 1).inv() else result valhrsh 1
            lng += dlng

            // Valhalla uses precision 6: divide by 1E6
            val finalLat = lat.toDouble() / 1000000.0
            val finalLng = lng.toDouble() / 1000000.0
            poly.add(GeoPoint(finalLat, finalLng))
        }
        return poly
    }

    private infix fun Int.valhrsh(bitCount: Int): Int = this ushr bitCount
}

// --- ROUTE NAVIGATION AND TRACKING UTILS ---

object RouteTracker {

    fun getDistanceToSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val x = p.longitude
        val y = p.latitude
        val x1 = a.longitude
        val y1 = a.latitude
        val x2 = b.longitude
        val y2 = b.latitude

        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) {
            return p.distanceToAsDouble(a)
        }

        val t = ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)
        return when {
            t < 0.0 -> p.distanceToAsDouble(a)
            t > 1.0 -> p.distanceToAsDouble(b)
            else -> {
                val projX = x1 + t * dx
                val projY = y1 + t * dy
                p.distanceToAsDouble(GeoPoint(projY, projX))
            }
        }
    }

    fun findClosestSegmentIndex(p: GeoPoint, points: List<GeoPoint>): Int {
        if (points.size < 2) return 0
        var minDistance = Double.MAX_VALUE
        var closestIndex = 0
        for (i in 0 until points.size - 1) {
            val dist = getDistanceToSegment(p, points[i], points[i + 1])
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }
        return closestIndex
    }

    fun getRouteSegmentBearing(a: GeoPoint, b: GeoPoint): Float {
        val lat1 = Math.toRadians(a.latitude)
        val lon1 = Math.toRadians(a.longitude)
        val lat2 = Math.toRadians(b.latitude)
        val lon2 = Math.toRadians(b.longitude)
        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val bearingRad = Math.atan2(y, x)
        return ((Math.toDegrees(bearingRad) + 360) % 360).toFloat()
    }
}
