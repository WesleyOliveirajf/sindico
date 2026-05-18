import React, { useState, useEffect } from 'react';
import { api } from './api.js';

export default function CompromissosPage() {
    const [compromissos, setCompromissos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [abaAtiva, setAbaAtiva] = useState('abertos');
    const [formData, setFormData] = useState({
        titulo: '',
        descricao: '',
        inicioEm: '',
        local: '',
        tipo: 'OUTROS'
    });

    useEffect(() => { carregarCompromissos(); }, []);

    const carregarCompromissos = async () => {
        try {
            setLoading(true);
            const data = await api.get('/api/compromissos');
            setCompromissos(data);
        } catch (err) {
            console.error('Erro ao carregar compromissos:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/compromissos', formData);
            setFormData({ titulo: '', descricao: '', inicioEm: '', local: '', tipo: 'OUTROS' });
            setShowForm(false);
            setAbaAtiva('abertos');
            carregarCompromissos();
        } catch (err) {
            console.error('Erro ao salvar compromisso:', err);
            alert('Erro ao salvar compromisso');
        }
    };

    const handleConcluir = async (id) => {
        try {
            await api.patch(`/api/compromissos/${id}/concluir`);
            carregarCompromissos();
        } catch (err) {
            console.error('Erro ao concluir compromisso:', err);
            alert('Erro ao concluir compromisso');
        }
    };

    const handleDelete = async (id) => {
        if (!confirm('Deseja excluir este compromisso?')) return;
        try {
            await api.delete(`/api/compromissos/${id}`);
            carregarCompromissos();
        } catch (err) {
            alert('Erro ao excluir compromisso');
        }
    };

    const formatData = (dt) => {
        if (!dt) return '';
        const d = new Date(dt);
        return d.toLocaleDateString('pt-BR');
    };

    const formatDataHora = (dt) => {
        if (!dt) return '';
        const d = new Date(dt);
        return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    };

    const isConcluido = (c) => c.status === 'CONCLUIDO' || c.concluido === true;

    const abertos = compromissos.filter(c => !isConcluido(c));
    const concluidos = compromissos.filter(c => isConcluido(c));
    const listaAtiva = abaAtiva === 'abertos' ? abertos : concluidos;

    if (loading) return <div className="p-8 text-center text-gray-500">Carregando...</div>;

    return (
        <div className="p-6">
            {/* Cabeçalho */}
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold text-gray-800">Compromissos</h1>
                <button
                    onClick={() => setShowForm(!showForm)}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                >
                    {showForm ? 'Cancelar' : '+ Novo Compromisso'}
                </button>
            </div>

            {/* Formulário */}
            {showForm && (
                <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 mb-6">
                    <h2 className="text-lg font-semibold mb-4">Novo Compromisso</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="md:col-span-2">
                            <label className="block text-sm font-medium text-gray-700 mb-1">Título *</label>
                            <input type="text" required value={formData.titulo}
                                onChange={e => setFormData({...formData, titulo: e.target.value})}
                                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Título do compromisso"
                            />
                        </div>
                        <div className="md:col-span-2">
                            <label className="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
                            <textarea value={formData.descricao}
                                onChange={e => setFormData({...formData, descricao: e.target.value})}
                                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                rows="2" placeholder="Descrição do compromisso"
                            />
                        </div>
                        {/* Apenas DATA de início — sem hora. Fim automático ao concluir. */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Data de Início *</label>
                            <input type="date" required value={formData.inicioEm}
                                onChange={e => setFormData({...formData, inicioEm: e.target.value})}
                                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Tipo</label>
                            <select value={formData.tipo}
                                onChange={e => setFormData({...formData, tipo: e.target.value})}
                                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            >
                                <option value="OUTROS">Outros</option>
                                <option value="MANUTENCAO">Manutenção</option>
                                <option value="REUNIAO">Reunião</option>
                            </select>
                        </div>
                        <div className="md:col-span-2">
                            <label className="block text-sm font-medium text-gray-700 mb-1">Local</label>
                            <input type="text" value={formData.local}
                                onChange={e => setFormData({...formData, local: e.target.value})}
                                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Local do compromisso"
                            />
                        </div>
                    </div>
                    <div className="flex gap-3 mt-4">
                        <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">
                            Salvar
                        </button>
                        <button type="button" onClick={() => setShowForm(false)} className="bg-gray-200 text-gray-700 px-6 py-2 rounded-lg hover:bg-gray-300">
                            Cancelar
                        </button>
                    </div>
                </form>
            )}

            {/* Abas */}
            <div className="flex border-b mb-6">
                <button
                    onClick={() => setAbaAtiva('abertos')}
                    className={`px-5 py-2 text-sm font-medium border-b-2 transition-colors ${
                        abaAtiva === 'abertos'
                            ? 'border-blue-600 text-blue-600'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    📋 A Fazer
                    {abertos.length > 0 && (
                        <span className="ml-2 bg-yellow-100 text-yellow-700 text-xs font-semibold px-2 py-0.5 rounded-full">
                            {abertos.length}
                        </span>
                    )}
                </button>
                <button
                    onClick={() => setAbaAtiva('concluidos')}
                    className={`px-5 py-2 text-sm font-medium border-b-2 transition-colors ${
                        abaAtiva === 'concluidos'
                            ? 'border-green-600 text-green-600'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    ✅ Concluídos
                    {concluidos.length > 0 && (
                        <span className="ml-2 bg-green-100 text-green-700 text-xs font-semibold px-2 py-0.5 rounded-full">
                            {concluidos.length}
                        </span>
                    )}
                </button>
            </div>

            {/* Lista */}
            {listaAtiva.length === 0 ? (
                <div className="text-center py-10 text-gray-400">
                    {abaAtiva === 'abertos' ? (
                        <>
                            <p className="text-lg">Nenhum compromisso em aberto.</p>
                            <p className="text-sm mt-1">Clique em "+ Novo Compromisso" para adicionar.</p>
                        </>
                    ) : (
                        <p className="text-lg">Nenhum compromisso concluído ainda.</p>
                    )}
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {listaAtiva.map(c => (
                        <div key={c.id} className={`bg-white rounded-lg shadow p-5 border-l-4 ${isConcluido(c) ? 'border-green-400' : 'border-yellow-400'}`}>
                            <div className="flex items-start gap-3 mb-3">
                                {!isConcluido(c) ? (
                                    <button
                                        title="Marcar como concluído"
                                        onClick={() => handleConcluir(c.id)}
                                        className="mt-1 w-5 h-5 flex-shrink-0 rounded border-2 border-gray-400 hover:border-green-500 hover:bg-green-50 transition-colors cursor-pointer"
                                        aria-label="Marcar como concluído"
                                    />
                                ) : (
                                    <span className="mt-1 w-5 h-5 flex-shrink-0 flex items-center justify-center rounded bg-green-500 text-white text-xs font-bold">✓</span>
                                )}
                                <div className="flex-1 min-w-0">
                                    <h3 className={`font-semibold text-base leading-tight ${isConcluido(c) ? 'line-through text-gray-400' : 'text-gray-800'}`}>
                                        {c.titulo}
                                    </h3>
                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium mt-1 inline-block ${
                                        isConcluido(c) ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                                    }`}>
                                        {isConcluido(c) ? 'Concluído' : 'Aberto'}
                                    </span>
                                </div>
                            </div>
                            {c.descricao && <p className="text-sm text-gray-600 mb-2">{c.descricao}</p>}
                            <div className="text-xs text-gray-500 space-y-1">
                                <p>📅 Início: {formatData(c.inicioEm)}</p>
                                {c.fimEm && <p>🏁 Concluído em: {formatDataHora(c.fimEm)}</p>}
                                {c.local && <p>📍 {c.local}</p>}
                                {c.tipo && c.tipo !== 'OUTROS' && <p>🏷️ {c.tipo === 'MANUTENCAO' ? 'Manutenção' : 'Reunião'}</p>}
                            </div>
                            <div className="flex gap-2 mt-4">
                                <button onClick={() => handleDelete(c.id)} className="text-red-500 hover:text-red-700 text-sm transition-colors">
                                    Excluir
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
