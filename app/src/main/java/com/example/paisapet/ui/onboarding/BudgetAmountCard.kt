package com.example.paisapet.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisapet.R
import com.example.paisapet.format.formatRupees
import com.example.paisapet.onboarding.BudgetCheck
import com.example.paisapet.onboarding.BudgetInput
import com.example.paisapet.onboarding.IndianGrouping
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.ChipSelectedBlue
import com.example.paisapet.ui.theme.HelperPill
import com.example.paisapet.ui.theme.MintText
import com.example.paisapet.ui.theme.SurfaceContainer
import java.time.LocalDate
import java.time.ZoneId

// The S2 budget input, shared by onboarding (S2) and the Settings budget sheet (S9). Moved here unchanged.

/** The big "₹5,000" field with its per-day helper or inline error. [text] is digits only, as BudgetInput.accept leaves it. */
@Composable
fun BudgetAmountCard(text: String, check: BudgetCheck, onTextChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "MONTHLY MISC ALLOWANCE",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = text,
                onValueChange = { onTextChange(BudgetInput.accept(text, it)) },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                visualTransformation = RupeeGroupingTransformation,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Monthly misc allowance in rupees" },
            )
            Spacer(Modifier.height(8.dp))
            // Fixed height so the card does not jump between the helper, an error and nothing.
            Box(modifier = Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
                when (check) {
                    is BudgetCheck.Valid -> PerDayHelper(check.paise / 100)
                    else -> BudgetInput.errorMessage(check)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun PerDayHelper(budgetRupees: Long) {
    val perDay = BudgetInput.approxPerDayRupees(budgetRupees, LocalDate.now(ZoneId.systemDefault()))
    Surface(shape = CircleShape, color = HelperPill) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MintText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Approx. ${formatRupees(perDay * 100)} / day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** ₹3,000 / ₹5,000 / ₹8,000. [selected] is the chip matching the text, if any. */
@Composable
fun BudgetChips(selected: Long?, onPick: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rupees in BudgetInput.CHIPS_RUPEES) {
            val isSelected = rupees == selected
            Surface(
                onClick = { onPick(rupees) },
                shape = CircleShape,
                color = if (isSelected) ChipSelectedBlue else SurfaceContainer,
                contentColor = if (isSelected) CardWhite else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics {
                        role = Role.Button
                        this.selected = isSelected
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        formatRupees(rupees * 100),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Shows "5000" as "₹5,000". The leading "₹" shifts every cursor offset by one. */
private object RupeeGroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                1 + IndianGrouping.originalToTransformed(digits, offset)

            override fun transformedToOriginal(offset: Int): Int =
                IndianGrouping.transformedToOriginal(digits, offset - 1)
        }
        return TransformedText(AnnotatedString("₹" + IndianGrouping.group(digits)), mapping)
    }
}
