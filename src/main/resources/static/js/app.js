class TaskManager {
    constructor() {
        this.baseUrl = '/api/tasks';
        this.currentTaskId = null;
        this.currentFilter = 'all';

        this.initializeElements();
        this.bindEvents();
        this.loadTasks();
    }

    initializeElements() {
        this.taskForm = document.getElementById('task-form');
        this.formTitle = document.getElementById('form-title');
        this.submitBtn = document.getElementById('submit-btn');
        this.cancelBtn = document.getElementById('cancel-btn');
        this.tasksTableBody = document.getElementById('tasks-table-body');
        this.noTasks = document.getElementById('no-tasks');
        this.formError = document.getElementById('form-error');
        this.deleteModal = document.getElementById('delete-modal');
        this.confirmDeleteBtn = document.getElementById('confirm-delete');
        this.cancelDeleteBtn = document.getElementById('cancel-delete');
        this.filterButtons = document.querySelectorAll('.filter-btn');
    }

    bindEvents() {
        this.taskForm.addEventListener('submit', (e) => this.handleSubmit(e));
        this.cancelBtn.addEventListener('click', () => this.cancelEdit());
        this.confirmDeleteBtn.addEventListener('click', () => this.performDelete());
        this.cancelDeleteBtn.addEventListener('click', () => this.closeDeleteModal());

        this.filterButtons.forEach(btn => {
            btn.addEventListener('click', (e) => this.filterTasks(e));
        });
    }

    async loadTasks() {
        try {
            const response = await fetch(this.baseUrl);
            if (!response.ok) throw new Error('Ошибка загрузки задач');

            const tasks = await response.json();
            this.displayTasks(tasks);
        } catch (error) {
            this.showError('Не удалось загрузить задачи');
            console.error('Error loading tasks:', error);
        }
    }

    displayTasks(tasks) {
        this.tasksTableBody.innerHTML = '';

        const filteredTasks = this.filterTasksByStatus(tasks);

        if (filteredTasks.length === 0) {
            this.noTasks.style.display = 'block';
            return;
        }

        this.noTasks.style.display = 'none';

        filteredTasks.forEach(task => {
            const row = this.createTaskRow(task);
            this.tasksTableBody.appendChild(row);
        });
    }

    createTaskRow(task) {
        const row = document.createElement('tr');

        const formattedDate = task.createdAt ?
            new Date(task.createdAt).toLocaleString('ru-RU') : '-';

        const formattedDueDate = task.dueDate ?
            new Date(task.dueDate).toLocaleString('ru-RU') : '-';

        row.innerHTML = `
            <td>${this.escapeHtml(task.title)}</td>
            <td>${this.escapeHtml(task.description || '')}</td>
            <td><span class="status-badge status-${task.status.toLowerCase()}">${task.status}</span></td>
            <td>${formattedDate}</td>
            <td>${formattedDueDate}</td>
            <td>
                <div class="actions">
                    <button class="action-btn edit-btn" data-id="${task.id}">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="action-btn delete-btn" data-id="${task.id}">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        `;

        row.querySelector('.edit-btn').addEventListener('click', () => this.editTask(task.id));
        row.querySelector('.delete-btn').addEventListener('click', () => this.showDeleteModal(task.id));

        return row;
    }

    filterTasksByStatus(tasks) {
        if (this.currentFilter === 'all') return tasks;
        return tasks.filter(task => task.status === this.currentFilter);
    }

    filterTasks(event) {
        const filter = event.target.dataset.filter;
        this.currentFilter = filter;

        this.filterButtons.forEach(btn => {
            btn.classList.remove('active');
        });

        event.target.classList.add('active');
        this.loadTasks();
    }

    async handleSubmit(event) {
        event.preventDefault();
        this.hideError();

        const formData = new FormData(this.taskForm);
        const taskData = {
            title: formData.get('title'),
            description: formData.get('description') || null,
            status: formData.get('status'),
            dueDate: formData.get('dueDate') || null
        };

        if (taskData.dueDate) {
            taskData.dueDate = new Date(taskData.dueDate).toISOString();
        }

        try {
            let response;

            if (this.currentTaskId) {
                response = await fetch(`${this.baseUrl}/${this.currentTaskId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(taskData)
                });
            } else {
                response = await fetch(this.baseUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(taskData)
                });
            }

            if (!response.ok) {
                const errorData = await response.json();
                this.handleValidationErrors(errorData);
                return;
            }

            const task = await response.json();

            this.showSuccess(this.currentTaskId ? 'Задача обновлена' : 'Задача создана');
            this.resetForm();
            this.loadTasks();

        } catch (error) {
            this.showError('Ошибка при сохранении задачи');
            console.error('Error saving task:', error);
        }
    }

    handleValidationErrors(errorData) {
        if (errorData.errors && Array.isArray(errorData.errors)) {
            const messages = errorData.errors.map(err =>
                `<strong>${err.field}</strong>: ${err.message}`
            ).join('<br>');

            this.showError(messages);
        } else {
            this.showError(errorData.message || 'Ошибка валидации');
        }
    }

    async editTask(taskId) {
        try {
            const response = await fetch(`${this.baseUrl}/${taskId}`);
            if (!response.ok) throw new Error('Задача не найдена');

            const task = await response.json();
            this.fillForm(task);
            this.currentTaskId = taskId;
            this.formTitle.innerHTML = '<i class="fas fa-edit"></i> Редактировать задачу';
            this.submitBtn.innerHTML = '<i class="fas fa-save"></i> Обновить';
            this.cancelBtn.style.display = 'inline-flex';

        } catch (error) {
            this.showError('Не удалось загрузить задачу для редактирования');
            console.error('Error loading task for edit:', error);
        }
    }

    fillForm(task) {
        document.getElementById('title').value = task.title;
        document.getElementById('description').value = task.description || '';
        document.getElementById('status').value = task.status;

        if (task.dueDate) {
            const dueDate = new Date(task.dueDate);
            const localDateTime = dueDate.toISOString().slice(0, 16);
            document.getElementById('dueDate').value = localDateTime;
        } else {
            document.getElementById('dueDate').value = '';
        }
    }

    cancelEdit() {
        this.resetForm();
    }

    resetForm() {
        this.taskForm.reset();
        this.currentTaskId = null;
        this.formTitle.innerHTML = '<i class="fas fa-plus"></i> Новая задача';
        this.submitBtn.innerHTML = '<i class="fas fa-save"></i> Сохранить';
        this.cancelBtn.style.display = 'none';
        this.hideError();
    }

    showDeleteModal(taskId) {
        this.currentTaskId = taskId;
        this.deleteModal.style.display = 'flex';
    }

    closeDeleteModal() {
        this.deleteModal.style.display = 'none';
        this.currentTaskId = null;
    }

    async performDelete() {
        try {
            const response = await fetch(`${this.baseUrl}/${this.currentTaskId}`, {
                method: 'DELETE'
            });

            if (!response.ok) throw new Error('Ошибка удаления');

            this.showSuccess('Задача удалена');
            this.closeDeleteModal();
            this.loadTasks();

        } catch (error) {
            this.showError('Ошибка при удалении задачи');
            console.error('Error deleting task:', error);
        }
    }

    showError(message) {
        this.formError.innerHTML = message;
        this.formError.style.display = 'block';
        this.formError.className = 'error-message';
    }

    showSuccess(message) {
        this.formError.innerHTML = message;
        this.formError.style.display = 'block';
        this.formError.className = 'success-message';

        setTimeout(() => {
            this.hideError();
        }, 3000);
    }

    hideError() {
        this.formError.style.display = 'none';
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Инициализация приложения при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    new TaskManager();
});