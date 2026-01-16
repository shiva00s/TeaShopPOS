package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.io.Serializable

enum class AttendanceType {
    WORK,
    GAP,
    SHOP_CLOSED_PAID,
    SHOP_CLOSED_UNPAID
}

@Keep
data class Attendance(
    @get:PropertyName("attendanceId") @set:PropertyName("attendanceId")
    var attendanceId: String = "",

    @get:PropertyName("employeeId") @set:PropertyName("employeeId")
    var employeeId: String = "",

    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",

    @get:PropertyName("checkInTime") @set:PropertyName("checkInTime")
    var checkInTime: Long = 0,

    @get:PropertyName("checkOutTime") @set:PropertyName("checkOutTime")
    var checkOutTime: Long? = null,

    @Deprecated("Use attendanceType instead")
    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "WORK", // WORK, GAP, etc.

    @get:PropertyName("attendanceType") @set:PropertyName("attendanceType")
    var attendanceType: AttendanceType = AttendanceType.WORK,

    // Snapshot of employee details at time of check-in
    @get:PropertyName("shiftStart") @set:PropertyName("shiftStart")
    var shiftStart: String = "",

    @get:PropertyName("shiftEnd") @set:PropertyName("shiftEnd")
    var shiftEnd: String = "",

    @get:PropertyName("breakHours") @set:PropertyName("breakHours")
    var breakHours: Double = 0.0,

    @get:PropertyName("salaryType") @set:PropertyName("salaryType")
    var salaryType: String = "",

    @get:PropertyName("salaryRate") @set:PropertyName("salaryRate")
    var salaryRate: Double = 0.0
) : Serializable