# Task: Implement API Services

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 3 hours  

## Objective

Implement API services for all backend microservices.

## Prerequisites

- [x] Task 003: Core module created

## Scope

**Files to Create**:
- `frontend/src/app/core/services/identity.service.ts`
- `frontend/src/app/core/services/organization.service.ts`
- `frontend/src/app/core/services/experiment.service.ts`
- `frontend/src/app/core/services/metrics.service.ts`

## Implementation Details

### Experiment Service

```typescript
@Injectable({ providedIn: 'root' })
export class ExperimentService {
  private apiUrl = `${environment.experimentServiceUrl}/experiments`;
  
  constructor(private http: HttpClient) {}
  
  getExperiments(): Observable<Experiment[]> {
    return this.http.get<Experiment[]>(this.apiUrl);
  }
  
  getExperiment(id: string): Observable<Experiment> {
    return this.http.get<Experiment>(`${this.apiUrl}/${id}`);
  }
  
  createExperiment(experiment: CreateExperimentRequest): Observable<Experiment> {
    return this.http.post<Experiment>(this.apiUrl, experiment);
  }
  
  startExperiment(id: string): Observable<Experiment> {
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/start`, {});
  }
  
  completeExperiment(id: string): Observable<Experiment> {
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/complete`, {});
  }
}
```

## Acceptance Criteria

- [ ] All API services implemented
- [ ] HTTP methods correct
- [ ] Error handling implemented
- [ ] Type safety enforced
- [ ] Services tested

## References

- Specification: `specs/angular-frontend.md` (API Services section)
- Related Tasks: 012-add-routing
