function initAutoCompleteEmail(myname, myvalue, mytype)
{
    $(myname).autocomplete(
        {
            source: function (request, response)
            {
                $.ajax(
                    {
                        url: "/emaillookup.jsp",
                        data: {
                            startsWith: request.term,
                            accountType: 2 //2 = person
                        },
                        type: "POST",  // POST transmits in querystring format (key=value&key1=value1) in utf-8
                        dataType: "json",   //return data in json format
                        success: function (data)
                        {
                            var results = [];

                            $.map(data.accounts, function (item)
                            {
                                var itemToAdd =  { label: item.label, id: item.value };
                                results.push(itemToAdd);
                            });

                            return response(results);
                        }
                    });
            },
            minLength: 1,
            delay: 450,
            selectFirst: false,
            autoFocus: true,
            select: function(event, ui)
            {
                $(myname).val(ui.item.label);
                $(myvalue).val(ui.item.id);
            }
        });
};
