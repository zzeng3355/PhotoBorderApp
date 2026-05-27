package com.zzeng.photoborder.data.model

import com.google.gson.annotations.SerializedName

data class Template(
    @SerializedName("template_name") val templateName: String,
    val version: String,
    val author: String,
    val description: String,
    @SerializedName("base_resolution") val baseResolution: Resolution,
    val layout: LayoutConfig,
    @SerializedName("brand_logos") val brandLogos: Map<String, BrandLogoConfig>,
    val fonts: Map<String, FontConfig>,
    @SerializedName("elements_landscape") val elementsLandscape: List<Element>,
    @SerializedName("elements_portrait_single_line") val elementsPortraitSingle: List<Element>,
    @SerializedName("elements_portrait_two_line") val elementsPortraitTwo: List<Element>,
    @SerializedName("exif_mapping") val exifMapping: Map<String, String>,
    @SerializedName("format_rules") val formatRules: Map<String, FormatRule>,
    @SerializedName("brand_model_map") val brandModelMap: Map<String, String>,
    @SerializedName("model_simplification") val modelSimplification: Map<String, String>
)

data class Resolution(
    val width: Int,
    val height: Int
)

data class LayoutConfig(
    @SerializedName("border_position") val borderPosition: String,
    @SerializedName("border_height_ratio_landscape") val borderHeightRatioLandscape: Float,
    @SerializedName("border_height_ratio_portrait") val borderHeightRatioPortrait: Float,
    @SerializedName("background_color") val backgroundColor: String,
    @SerializedName("side_margin_ratio") val sideMarginRatio: Float,
    @SerializedName("center_gap_ratio") val centerGapRatio: Float
)

data class BrandLogoConfig(
    val file: String,
    @SerializedName("height_ratio") val heightRatio: Float,
    @SerializedName("baseline_offset") val baselineOffset: Int
)

data class FontConfig(
    val family: String,
    @SerializedName("size_ratio") val sizeRatio: Float,
    val color: String
)

data class Element(
    val id: String,
    val type: String,
    val content: String? = null,
    val position: String,
    @SerializedName("font_ref") val fontRef: String? = null,
    @SerializedName("y_ratio") val yRatio: Float? = null,
    val align: String? = null,
    @SerializedName("baseline_align") val baselineAlign: Boolean? = null,
    @SerializedName("special_elements") val specialElements: List<SpecialElement>? = null
)

data class SpecialElement(
    val type: String,
    val text: String,
    val color: String,
    @SerializedName("overlap_ratio") val overlapRatio: Float,
    @SerializedName("font_ref") val fontRef: String
)

data class FormatRule(
    val prefix: String? = null,
    val suffix: String? = null,
    @SerializedName("remove_decimal_if_whole") val removeDecimalIfWhole: Boolean? = null,
    val format: String? = null,
    @SerializedName("round_to_int") val roundToInt: Boolean? = null,
    @SerializedName("portrait_compact") val portraitCompact: Boolean? = null
)
