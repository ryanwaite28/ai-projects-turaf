# Task: Implement Problems Module

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 3 hours  

## Objective

Implement problems module with list, create, edit, and detail views.

## Prerequisites

- [x] Task 005: Dashboard module implemented

## Scope

**Files to Create**:
- `frontend/src/app/features/problems/problems.module.ts`
- `frontend/src/app/features/problems/problem-list/problem-list.component.ts`
- `frontend/src/app/features/problems/problem-detail/problem-detail.component.ts`
- `frontend/src/app/features/problems/problem-form/problem-form.component.ts`

## Implementation Details

### Problem List Component

```typescript
@Component({
  selector: 'app-problem-list',
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Problems</mat-card-title>
        <button mat-raised-button color="primary" (click)="createProblem()">
          Create Problem
        </button>
      </mat-card-header>
      <mat-card-content>
        <mat-table [dataSource]="problems$ | async">
          <ng-container matColumnDef="title">
            <mat-header-cell *matHeaderCellDef>Title</mat-header-cell>
            <mat-cell *matCellDef="let problem">{{problem.title}}</mat-cell>
          </ng-container>
          <ng-container matColumnDef="createdAt">
            <mat-header-cell *matHeaderCellDef>Created</mat-header-cell>
            <mat-cell *matCellDef="let problem">{{problem.createdAt | date}}</mat-cell>
          </ng-container>
          <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
          <mat-row *matRowDef="let row; columns: displayedColumns;" 
                   (click)="viewProblem(row)"></mat-row>
        </mat-table>
      </mat-card-content>
    </mat-card>
  `
})
export class ProblemListComponent implements OnInit {
  problems$ = this.store.select(selectAllProblems);
  displayedColumns = ['title', 'createdAt'];
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  ngOnInit() {
    this.store.dispatch(loadProblems());
  }
  
  createProblem() {
    this.router.navigate(['/problems/new']);
  }
  
  viewProblem(problem: Problem) {
    this.router.navigate(['/problems', problem.id]);
  }
}
```

## Acceptance Criteria

- [x] Problems module created
- [x] List view implemented
- [x] Create form implemented
- [x] Edit form implemented
- [x] Detail view implemented
- [x] CRUD operations work
- [x] NgRx integration complete

## Testing Requirements

**Unit Tests**:
- Test problem list
- Test problem form
- Test problem detail

## References

- Specification: `specs/angular-frontend.md` (Problems Module section)
- Related Tasks: 007-implement-hypotheses-module
