package security

import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import play.filters.gzip.GzipFilter
import javax.inject.Inject

class Filters @Inject() (csrfFilter: CSRFFilter, gzipFilter: GzipFilter) extends DefaultHttpFilters(csrfFilter,gzipFilter)
