package dev.antomanc.tameki.feature.aibridge

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.antomanc.tameki.core.model.ai.Problem
import dev.antomanc.tameki.core.model.ai.ProblemCode

@Composable
internal fun localizedProblem(problem: Problem): String {
    val arg = problem.arg.orEmpty()
    return when (problem.code) {
        ProblemCode.NO_JSON -> stringResource(R.string.ai_problem_no_json)
        ProblemCode.INVALID_JSON -> stringResource(R.string.ai_problem_invalid_json)
        ProblemCode.MISSING_VERSION -> stringResource(R.string.ai_problem_missing_version)
        ProblemCode.VERSION_NOT_NUMBER -> stringResource(R.string.ai_problem_version_not_number)
        ProblemCode.UNSUPPORTED_VERSION -> stringResource(R.string.ai_problem_unsupported_version, arg)
        ProblemCode.EXPECTED_OBJECT -> stringResource(R.string.ai_problem_expected_object)
        ProblemCode.UNKNOWN_KIND -> stringResource(R.string.ai_problem_unknown_kind)
        ProblemCode.MISSING_AMOUNT -> stringResource(R.string.ai_problem_missing_amount)
        ProblemCode.AMOUNT_TYPE -> stringResource(R.string.ai_problem_amount_type)
        ProblemCode.CANNOT_PARSE_AMOUNT -> stringResource(R.string.ai_problem_cannot_parse_amount, arg)
        ProblemCode.MISSING_NAME -> stringResource(R.string.ai_problem_missing_name)
        ProblemCode.MISSING_CYCLE_DEFAULT -> stringResource(R.string.ai_problem_missing_cycle)
        ProblemCode.UNRECOGNIZED_CYCLE -> stringResource(R.string.ai_problem_unrecognized_cycle, arg)
        ProblemCode.INVALID_DATE -> stringResource(R.string.ai_problem_invalid_date, arg)
        ProblemCode.INVALID_TIME -> stringResource(R.string.ai_problem_invalid_time, arg)
        ProblemCode.UNKNOWN_CATEGORY -> stringResource(R.string.ai_problem_unknown_category, arg)
        ProblemCode.UNKNOWN_ACCOUNT -> stringResource(R.string.ai_problem_unknown_account, arg)
        ProblemCode.NO_ACCOUNT -> stringResource(R.string.ai_problem_no_account)
    }
}
