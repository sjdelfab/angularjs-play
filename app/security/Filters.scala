package security

import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter
import javax.inject.Inject

class Filters @Inject() (csrfFilter: CSRFFilter, 
                         securityHeadersFilter: SecurityHeadersFilter, 
                         gzipFilter: GzipFilter) extends DefaultHttpFilters(csrfFilter,securityHeadersFilter,gzipFilter)
