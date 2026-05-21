import { useCallback, useEffect, useState } from 'react'
import {
  aprovarUsuario,
  getAdminStats,
  getAdminUsuarios,
  reativarUsuario,
  rejeitarUsuario,
} from './api'
import ConfigIAPage from './ConfigIAPage'

const STATUS_LABEL = {
  ativo: 'Ativo',
  pendente: 'Pendente',
  inativo: 'Inativo',
}

const STATUS_CLASS = {
  ativo: 'badge badge--ativo',
  pendente: 'badge badge--pendente',
  inativo: 'badge badge--inativo',
}

function fmtDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function StatCard({ label, value, highlight }) {
  return (
    <div className={`stat-card${highlight ? ' stat-card--highlight' : ''}`}>
      <span className="stat-card__value">{value ?? '—'}</span>
      <span className="stat-card__label">{label}</span>
    </div>
  )
}

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState('usuarios')
  const [stats, setStats] = useState(null)
  const [usuarios, setUsuarios] = useState([])
  const [loadingStats, setLoadingStats] = useState(true)
  const [loadingUsuarios, setLoadingUsuarios] = useState(true)
  const [errorStats, setErrorStats] = useState(null)
  const [errorUsuarios, setErrorUsuarios] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [actionLoading, setActionLoading] = useState(null) // id being processed

  const fetchStats = useCallback(async () => {
    setLoadingStats(true)
    setErrorStats(null)
    try {
      const data = await getAdminStats()
      setStats(data)
    } catch (e) {
      setErrorStats(e.message)
    } finally {
      setLoadingStats(false)
    }
  }, [])

  const fetchUsuarios = useCallback(async () => {
    setLoadingUsuarios(true)
    setErrorUsuarios(null)
    try {
      const data = await getAdminUsuarios()
      setUsuarios(data)
    } catch (e) {
      setErrorUsuarios(e.message)
    } finally {
      setLoadingUsuarios(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchStats()
    fetchUsuarios()
  }, [fetchStats, fetchUsuarios])

  async function handleAction(fn, id) {
    setActionError(null)
    setActionLoading(id)
    try {
      await fn(id)
      await Promise.all([fetchStats(), fetchUsuarios()])
    } catch (e) {
      setActionError(e.message)
    } finally {
      setActionLoading(null)
    }
  }

  const pendentes = usuarios.filter((u) => u.status === 'pendente')

  return (
    <div className="admin-page">
      <h1 className="page-title">Painel Administrativo</h1>

      <div className="admin-tabs" role="tablist" aria-label="Secoes administrativas">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'usuarios'}
          className={`admin-tab${activeTab === 'usuarios' ? ' admin-tab--active' : ''}`}
          onClick={() => setActiveTab('usuarios')}
        >
          Usuarios
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'ia'}
          className={`admin-tab${activeTab === 'ia' ? ' admin-tab--active' : ''}`}
          onClick={() => setActiveTab('ia')}
        >
          IA
        </button>
      </div>

      {activeTab === 'ia' ? (
        <section className="admin-section">
          <h2 className="admin-section__title">Configuracao do assistente</h2>
          <p className="muted">
            Escolha o provedor, modelo e credenciais que o assistente e os recursos de IA vao usar.
          </p>
          <ConfigIAPage embedded />
        </section>
      ) : (
        <>
      {/* Stats */}
      <section className="admin-section">
        <h2 className="admin-section__title">Visao Geral</h2>
        {errorStats && <p className="error-msg">{errorStats}</p>}
        {loadingStats ? (
          <p className="muted">Carregando...</p>
        ) : (
          <div className="stat-cards">
            <StatCard label="Total de usuarios" value={stats?.totalUsuarios} />
            <StatCard label="Ativos" value={stats?.ativos} />
            <StatCard label="Pendentes" value={stats?.pendentes} highlight={stats?.pendentes > 0} />
            <StatCard label="Inativos" value={stats?.inativos} />
            <StatCard label="Condominios" value={stats?.totalCondominios} />
            <StatCard label="Online (15 min)" value={stats?.onlineAgora} />
          </div>
        )}
      </section>

      {/* Pending approvals */}
      {pendentes.length > 0 && (
        <section className="admin-section">
          <h2 className="admin-section__title">Aprovacoes Pendentes ({pendentes.length})</h2>
          {actionError && <p className="error-msg">{actionError}</p>}
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Condominio</th>
                  <th>Cadastro</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {pendentes.map((u) => (
                  <tr key={u.id}>
                    <td>{u.nome || '—'}</td>
                    <td>{u.email}</td>
                    <td>{u.condominioNome || '—'}</td>
                    <td>{fmtDate(u.createdAt)}</td>
                    <td className="action-cell">
                      <button
                        className="btn btn--sm btn--success"
                        disabled={actionLoading === u.id}
                        onClick={() => handleAction(aprovarUsuario, u.id)}
                      >
                        Aprovar
                      </button>
                      <button
                        className="btn btn--sm btn--danger"
                        disabled={actionLoading === u.id}
                        onClick={() => handleAction(rejeitarUsuario, u.id)}
                      >
                        Rejeitar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* All users */}
      <section className="admin-section">
        <h2 className="admin-section__title">Todos os Usuarios</h2>
        {errorUsuarios && <p className="error-msg">{errorUsuarios}</p>}
        {loadingUsuarios ? (
          <p className="muted">Carregando...</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Condominio</th>
                  <th>Perfil</th>
                  <th>Status</th>
                  <th>Ultimo acesso</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map((u) => (
                  <tr key={u.id}>
                    <td>{u.nome || '—'}</td>
                    <td>{u.email}</td>
                    <td>{u.condominioNome || '—'}</td>
                    <td>{u.perfil || '—'}</td>
                    <td>
                      <span className={STATUS_CLASS[u.status] || 'badge'}>
                        {STATUS_LABEL[u.status] || u.status}
                      </span>
                    </td>
                    <td>{fmtDate(u.ultimoAcesso)}</td>
                    <td className="action-cell">
                      {u.status === 'pendente' && (
                        <>
                          <button
                            className="btn btn--sm btn--success"
                            disabled={actionLoading === u.id}
                            onClick={() => handleAction(aprovarUsuario, u.id)}
                          >
                            Aprovar
                          </button>
                          <button
                            className="btn btn--sm btn--danger"
                            disabled={actionLoading === u.id}
                            onClick={() => handleAction(rejeitarUsuario, u.id)}
                          >
                            Rejeitar
                          </button>
                        </>
                      )}
                      {u.status === 'ativo' && u.perfil !== 'ADMIN' && (
                        <button
                          className="btn btn--sm btn--danger"
                          disabled={actionLoading === u.id}
                          onClick={() => handleAction(rejeitarUsuario, u.id)}
                        >
                          Desativar
                        </button>
                      )}
                      {u.status === 'inativo' && (
                        <button
                          className="btn btn--sm btn--success"
                          disabled={actionLoading === u.id}
                          onClick={() => handleAction(reativarUsuario, u.id)}
                        >
                          Reativar
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
        </>
      )}
    </div>
  )
}
