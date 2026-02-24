const API = '/api';

const app = {

    async init() {
        await this.cargarCuentas();
        await this.cargarHistorial();
        await this.cargarEstadisticas();
        await this.cargarEstadoConfig();
    },

    async cargarCuentas() {
        try {
            const res  = await fetch(`${API}/cuentas`);
            const data = await res.json();

            const selOrigen  = document.getElementById('cuentaOrigen');
            const selDestino = document.getElementById('cuentaDestino');
            selOrigen.innerHTML  = '<option value="">Seleccione cuenta de origen</option>';
            selDestino.innerHTML = '<option value="">Seleccione cuenta de destino</option>';

            data.bancoNacional.forEach(c => {
                selOrigen.innerHTML +=
                    `<option value="${c.numeroCuenta}">${c.numeroCuenta} — ${c.titular} ($ ${this.fmt(c.saldo)})</option>`;
            });
            data.bancoInternacional.forEach(c => {
                selDestino.innerHTML +=
                    `<option value="${c.numeroCuenta}">${c.numeroCuenta} — ${c.titular} ($ ${this.fmt(c.saldo)})</option>`;
            });

            this.renderCuentas('listaNacional',       data.bancoNacional);
            this.renderCuentas('listaInternacional',  data.bancoInternacional);

        } catch (e) {
            this.showAlert('error', 'No se pudo conectar con el servidor. Verifique que la aplicación esté en ejecución.');
        }
    },

    renderCuentas(id, cuentas) {
        const el = document.getElementById(id);
        if (!cuentas || cuentas.length === 0) {
            el.innerHTML = '<div class="loading-row">Sin cuentas disponibles</div>';
            return;
        }
        el.innerHTML = cuentas.map(c => `
            <div class="account-item">
                <div>
                    <div class="account-num">${c.numeroCuenta}</div>
                    <div class="account-name">${c.titular}</div>
                </div>
                <div class="account-balance">$${this.fmt(c.saldo)}</div>
            </div>
        `).join('');
    },

    async realizarTransferencia() {
        const cuentaOrigen  = document.getElementById('cuentaOrigen').value;
        const cuentaDestino = document.getElementById('cuentaDestino').value;
        const monto         = parseFloat(document.getElementById('monto').value);
        const descripcion   = document.getElementById('descripcion').value;

        if (!cuentaOrigen)        return this.showAlert('error', 'Seleccione una cuenta de origen.');
        if (!cuentaDestino)       return this.showAlert('error', 'Seleccione una cuenta de destino.');
        if (!monto || monto <= 0) return this.showAlert('error', 'Ingrese un monto válido mayor a cero.');

        this.hideAlerts();

        const btn = document.getElementById('btnTransferir');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>Procesando...';

        try {
            const res  = await fetch(`${API}/transferencias`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cuentaOrigen, cuentaDestino, monto, descripcion })
            });
            const data = await res.json();

            if (data.estado === 'COMPLETADA') {
                this.showAlert('success',
                    `Transferencia completada exitosamente.<br><small>Referencia: <strong>${data.referencia}</strong></small>`);
                document.getElementById('monto').value = '';
                document.getElementById('descripcion').value = '';
            } else if (data.estado === 'REVERTIDA') {
                this.showAlert('warning',
                    `Transferencia revertida. El débito fue compensado.<br><small>Referencia: <strong>${data.referencia}</strong></small>`);
            } else {
                this.showAlert('error',
                    `${data.mensaje || data.error || 'La operación no pudo completarse.'}<br><small>Referencia: <strong>${data.referencia || 'N/A'}</strong></small>`);
            }

            await this.cargarCuentas();
            await this.cargarHistorial();
            await this.cargarEstadisticas();

        } catch (e) {
            this.showAlert('error', 'Error de conexión con el servidor.');
        } finally {
            btn.disabled = false;
            btn.innerHTML = 'Autorizar Transferencia';
        }
    },

    async cargarHistorial() {
        try {
            const res  = await fetch(`${API}/transferencias`);
            const data = await res.json();
            const tbody = document.getElementById('historialBody');

            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="empty-row">No hay operaciones registradas</td></tr>';
                return;
            }

            tbody.innerHTML = data.map(t => `
                <tr>
                    <td><span class="ref-code">${t.referencia}</span></td>
                    <td>${t.cuentaOrigen}</td>
                    <td>${t.cuentaDestino}</td>
                    <td class="amount-cell">$${this.fmt(t.monto)}</td>
                    <td>${t.fechaCreacion || '—'}</td>
                    <td><span class="status-badge status-${t.estado}">${t.estado}</span></td>
                </tr>
            `).join('');

        } catch (e) {
            console.error('Error cargando historial:', e);
        }
    },

    async cargarEstadisticas() {
        try {
            const res  = await fetch(`${API}/stats`);
            const data = await res.json();
            document.getElementById('stat-total').textContent       = data.totalTransferencias;
            document.getElementById('stat-completadas').textContent  = data.completadas;
            document.getElementById('stat-fallidas').textContent     = (data.fallidas || 0) + (data.revertidas || 0);
            document.getElementById('stat-monto').textContent        = '$' + this.fmt(data.montoTotal || 0);
        } catch (e) {
            console.error('Error cargando estadísticas:', e);
        }
    },

    async toggleSimulacion() {
        const activo = document.getElementById('toggleFallo').checked;
        try {
            await fetch(`${API}/config/simular-fallo?activo=${activo}`, { method: 'POST' });
        } catch (e) {}
    },

    async cargarEstadoConfig() {
        try {
            const res  = await fetch(`${API}/config/estado`);
            const data = await res.json();
            document.getElementById('toggleFallo').checked = data.simulacionFallos;
        } catch (e) {}
    },

    showAlert(type, message) {
        this.hideAlerts();
        const el = document.getElementById(`alert${type.charAt(0).toUpperCase() + type.slice(1)}`);
        el.innerHTML = message;
        el.style.display = 'block';
    },

    hideAlerts() {
        ['alertSuccess', 'alertError', 'alertWarning'].forEach(id => {
            document.getElementById(id).style.display = 'none';
        });
    },

    fmt(n) {
        return parseFloat(n).toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
};

window.addEventListener('DOMContentLoaded', () => app.init());
