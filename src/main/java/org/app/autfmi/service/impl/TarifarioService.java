package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.TarifarioDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.TarifarioListResponse;
import org.app.autfmi.repository.TarifarioRepository;
import org.app.autfmi.service.ITarifarioService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarifarioService implements ITarifarioService {
    private final TarifarioRepository tarifarioRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse listTarifario(String token, Integer idCliente) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_TARIFARIO);
        BaseResponse response = tarifarioRepository.listaTarifario(baseRequest, idCliente);

        // El reclutador no ve las tarifas, y ocultarlas sólo en el frontend deja
        // el importe viajando en la respuesta. Aquí se van de verdad.
        //
        // La lista no puede devolverse vacía: es también el catálogo de perfiles
        // del cliente que alimenta el select de vacantes. Por eso se devuelven
        // los perfiles con los importes en null; el SP de alta lee ese null como
        // "usa la tarifa del tarifario".
        if (Common.esReclutador(user) && response instanceof TarifarioListResponse) {
            TarifarioListResponse lista = (TarifarioListResponse) response;
            lista.setLstTarifario(sinImportes(lista.getLstTarifario()));
        }

        return response;
    }

    /** Deja de cada fila sólo el perfil: id y nombre. */
    private List<TarifarioDTO> sinImportes(List<TarifarioDTO> tarifario) {
        if (tarifario == null) {
            return null;
        }
        return tarifario.stream()
                .map(t -> new TarifarioDTO(t.getIdPerfil(), t.getPerfil(), null, null, null, null, null))
                .collect(Collectors.toList());
    }

}
